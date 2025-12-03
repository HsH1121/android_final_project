from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from newscrawler.naver_api import NaverNewsCrawler
from ai.gemini_ai import GeminiAI
from dotenv import load_dotenv
import os

# --- .env 파일 로드 --- #
load_dotenv()

app = FastAPI()

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- .env에서 값 불러오기 --- #
client_id = os.getenv("NAVER_CLIENT_ID")
client_secret = os.getenv("NAVER_CLIENT_SECRET")
gemini_api_key = os.getenv("GEMINI_API_KEY")
print("Loaded KEY:", gemini_api_key)


# --- 객체 생성 --- #
crawler = NaverNewsCrawler(client_id, client_secret)
ai = GeminiAI(gemini_api_key)


# --- 텍스트 분석 API --- #
@app.post("/analyze")
async def analyze(payload: dict):
    text = payload.get("text")

    if not text:
        return {"analysis": None}

    result = ai.analyze_text(text)
    return {"analysis": result}


# --- 뉴스 검색 API --- #
@app.get("/news")
async def news(q: str, display: int = 10):
    result = crawler.search(q, display)
    return {"news": result}
