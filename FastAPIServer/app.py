import asyncio
import json
import time
from typing import Dict, List, Set

from fastapi import (
    FastAPI,
    WebSocket,
    WebSocketDisconnect,
    Query,
    Depends,
    HTTPException,
    status,
)
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
import os
from sqlalchemy.orm import Session
from sqlalchemy import or_

from db import Base, engine, get_db
from models import User
from schemas import (
    SignupRequest,
    SignupResponse,
    UserExistsResponse,
    LoginRequest,
    LoginResponse,
    Article,
    FactCheckRequest,
    FactCheckResponse,
    AnalyzeRequest,
    AnalyzeResponse,
)

from naver_news import NaverNewsCrawler, fetch_article_content
from gemini import fact_check_claim, generate_with_gemini

app = FastAPI(title="FastAPIServer")

# CORS 설정 (안드로이드 에뮬레이터에서 호출 편하게)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 네이버 뉴스 크롤러 준비 (환경 변수에서 키 읽기)
NAVER_CLIENT_ID = os.getenv("NAVER_CLIENT_ID", "Hzw3dCM4tPwnANV_F_Zy")
NAVER_CLIENT_SECRET = os.getenv("NAVER_CLIENT_SECRET", "9aABdVnll1")

news_crawler = NaverNewsCrawler(NAVER_CLIENT_ID, NAVER_CLIENT_SECRET)

# ====== DB 초기화 ======
@app.on_event("startup")
def on_startup():
    # 서버 시작 시 users 테이블 없으면 생성
    Base.metadata.create_all(bind=engine)


# ====== WebSocket용 상태 ======
# userId -> 현재 열린 WebSocket 세션들(기기 여러 대 대비)
_clients: Dict[str, Set[WebSocket]] = {}
# userId -> 오프라인/미수신 메시지 임시 보관(메모리)
_pending: Dict[str, List[dict]] = {}


def now_ms() -> int:
    return int(time.time() * 1000)


async def _safe_send(ws: WebSocket, payload: dict):
    """개별 세션에 JSON 텍스트 전송"""
    try:
        await ws.send_text(json.dumps(payload, ensure_ascii=False))
    except Exception:
        # 전송 실패 시 무시(연결 끊김 등)
        pass


async def _broadcast(to_user: str, payload: dict):
    """to_user에게 연결된 모든 세션으로 브로드캐스트 (동일 유저 다중기기 대비)"""
    sessions = _clients.get(to_user)
    if not sessions:
        # 오프라인이면 메모리 큐에 적재
        _pending.setdefault(to_user, []).append(payload)
        return
    await asyncio.gather(*[_safe_send(ws, payload) for ws in list(sessions)])


# ====== WebSocket 엔드포인트 ======
@app.websocket("/ws/chat")
async def chat(ws: WebSocket, userId: str = Query(..., description="현재 접속 유저 ID")):
    # 연결 수락 및 등록
    await ws.accept()
    _clients.setdefault(userId, set()).add(ws)

    # 접속 직후, 보류 메시지가 있으면 밀어주기
    queued = _pending.pop(userId, [])
    for m in queued:
        await _safe_send(ws, m)

    # 상태 알림(선택)
    await _safe_send(
        ws,
        {
            "type": "system",
            "event": "joined",
            "userId": userId,
            "serverTs": now_ms(),
        },
    )

    try:
        while True:
            text = await ws.receive_text()  # 클라이언트가 보낸 문자열(JSON 권장)
            try:
                msg = json.loads(text)
            except json.JSONDecodeError:
                # JSON이 아니면 에코(디버깅용)
                await _safe_send(
                    ws, {"type": "echo", "text": text, "serverTs": now_ms()}
                )
                continue

            mtype = msg.get("type")
            # 기본 메시지 포맷 예:
            # { "type":"message", "to":"userB", "text":"안녕", "clientMsgId":"uuid" }
            if mtype == "message":
                to_user = msg.get("to")
                payload = {
                    "type": "message",
                    "from": userId,
                    "to": to_user,
                    "text": msg.get("text", ""),
                    "clientMsgId": msg.get("clientMsgId"),
                    "serverTs": now_ms(),
                }
                # 수신자에게 전달
                await _broadcast(to_user, payload)
                # 발신자에게 전달완료(delivered) 이벤트 회신(선택)
                await _safe_send(
                    ws,
                    {
                        "type": "delivered",
                        "clientMsgId": msg.get("clientMsgId"),
                        "to": to_user,
                        "serverTs": now_ms(),
                    },
                )
            elif mtype == "typing":
                # 타이핑 표시 이벤트
                to_user = msg.get("to")
                await _broadcast(
                    to_user,
                    {
                        "type": "typing",
                        "from": userId,
                        "to": to_user,
                        "serverTs": now_ms(),
                    },
                )
            elif mtype == "read":
                # 읽음 처리 이벤트
                to_user = msg.get("to")
                await _broadcast(
                    to_user,
                    {
                        "type": "read",
                        "from": userId,
                        "to": to_user,
                        "clientMsgId": msg.get("clientMsgId"),
                        "serverTs": now_ms(),
                    },
                )
            else:
                # 알 수 없는 타입은 에코
                await _safe_send(
                    ws, {"type": "echo", "json": msg, "serverTs": now_ms()}
                )

    except WebSocketDisconnect:
        pass
    finally:
        # 세션 정리
        if userId in _clients:
            _clients[userId].discard(ws)
            if not _clients[userId]:
                _clients.pop(userId, None)


# ====== 보류 메시지 동기화 & 헬스체크 ======
@app.get("/sync")
def sync(userId: str, since: int = 0):
    """
    간단한 동기화용(옵션): 현재 구현은 메모리 큐만 사용.
    실제 DB가 없다 보니 since는 무시하고, 대기열이 있으면 반환 후 비웁니다.
    """
    msgs = _pending.pop(userId, [])
    return JSONResponse({"items": msgs, "serverTs": now_ms()})


@app.get("/health")
def health():
    return {"ok": True, "clients": {k: len(v) for k, v in _clients.items()}}


# ====== HTTP 엔드포인트들 (회원가입 / 로그인 / 유저 조회) ======
@app.post("/signup", response_model=SignupResponse)
def signup(req: SignupRequest, db: Session = Depends(get_db)):
    """
    회원가입: username, phone 중복이면 400 에러
    """
    # username 또는 phone 중복 체크
    exists = db.query(User).filter(
        or_(User.username == req.username, User.phone == req.phone)
    ).first()
    if exists:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="이미 사용 중인 username 또는 phone 입니다.",
        )

    user = User(
        username=req.username,
        password=req.password,  # 실제 서비스면 해시해야 함
        phone=req.phone,
        nickname=req.nickname,
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    return SignupResponse(
        id=user.id,
        username=user.username,
        phone=user.phone,
        nickname=user.nickname,
    )


@app.post("/login", response_model=LoginResponse)
def login(req: LoginRequest, db: Session = Depends(get_db)):
    """
    로그인: username + password 기준으로 서버 DB(users 테이블)에서 검증
    """
    user = db.query(User).filter(User.username == req.username).first()

    if not user or user.password != req.password:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="아이디 또는 비밀번호가 올바르지 않습니다.",
        )

    return LoginResponse(
        id=user.id,
        username=user.username,
        phone=user.phone,
        nickname=user.nickname,
    )


@app.get("/users/exists", response_model=UserExistsResponse)
def user_exists(phone: str, db: Session = Depends(get_db)):
    """
    전화번호로 가입된 유저가 있는지 확인:
    - 있으면 exists=True, nickname 반환
    - 없으면 exists=False
    """
    user = db.query(User).filter(User.phone == phone).first()
    if not user:
        return UserExistsResponse(exists=False)
    return UserExistsResponse(exists=True, nickname=user.nickname)

@app.post("/fact-check", response_model=FactCheckResponse)
async def fact_check(req: FactCheckRequest):
    """
    채팅 중 나온 주장(문장)을 받아
    - 네이버 뉴스에서 최신 기사 5개 검색
    - 각 기사 원문을 크롤링
    - Gemini로 사실 여부/논리적 허점 분석
    을 수행한 뒤 결과를 반환.
    """
    claim = req.claim.strip()
    if not claim:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="claim must not be empty",
        )

    # 1) 뉴스 검색
    news_items = news_crawler.search(claim, display=5)

    # 2) 각 기사 원문 크롤링
    articles: list[Article] = []
    for item in news_items:
        content = fetch_article_content(item.get("link", ""))
        articles.append(
            Article(
                title=item.get("title", ""),
                link=item.get("link", ""),
                desc=item.get("desc", ""),
                content=content,
            )
        )

    # 3) Gemini로 팩트체크 분석
    analysis_text = fact_check_claim(
        claim,
        [
            {
                "title": a.title,
                "link": a.link,
                "desc": a.desc,
                "content": a.content,
            }
            for a in articles
        ],
    )

    # 4) verdict 간단 추출 (텍스트 패턴)
    verdict = "unknown"
    if analysis_text:
        lower = analysis_text.lower()
        if (
            "거짓일 가능성이 높습니다" in analysis_text
            or "거짓" in analysis_text
            or "false" in lower
        ):
            verdict = "false"
        elif (
            "사실로 보입니다" in analysis_text
            or "사실" in analysis_text
            or "true" in lower
        ):
            verdict = "true"
        elif (
            "불확실" in analysis_text
            or "확실하지" in analysis_text
            or "uncertain" in lower
        ):
            verdict = "uncertain"

    return FactCheckResponse(
        claim=claim,
        verdict=verdict,
        analysis=analysis_text or "",
        articles=articles,
    )

@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze(payload: AnalyzeRequest):
    """
    일반 텍스트 분석 (예: 문장 분석 화면에서 호출)
    """
    text = (payload.text or "").strip()
    if not text:
        return AnalyzeResponse(analysis=None)

    result = generate_with_gemini(text)
    return AnalyzeResponse(analysis=result)


@app.get("/news")
async def news(q: str, display: int = 10):
    """
    뉴스 검색 (shyu_android에서 쓰던 /news 그대로)
    """
    items = news_crawler.search(q, display)
    # 기존 back/app 처럼 {"news": [...]} 형태로 반환
    return {"news": items}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
