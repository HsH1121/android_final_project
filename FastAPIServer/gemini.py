from typing import List, Dict
import os

from google import genai
from dotenv import load_dotenv  # ★ 추가

# .env 로드
load_dotenv()  # ★ 현재 폴더(.env) 읽어서 환경변수에 올림

# 환경 변수에서 API 키를 읽어옴
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

# 사용 모델 이름
MODEL_NAME = "gemini-2.5-flash"

_client: genai.Client | None = None


def _get_client() -> genai.Client:
    global _client
    if _client is None:
        if not GEMINI_API_KEY:
            raise RuntimeError("GEMINI_API_KEY is none")
        _client = genai.Client(api_key=GEMINI_API_KEY)
    return _client


def generate_with_gemini(prompt: str) -> str:
    """단순 프롬프트 → 응답 텍스트"""
    client = _get_client()
    response = client.models.generate_content(
        model=MODEL_NAME,
        contents=prompt,
    )
    return response.text


def fact_check_claim(claim: str, articles: List[Dict[str, str]]) -> str:
    """뉴스 기사 본문들을 바탕으로 주장의 사실 여부/허점 분석"""
    blocks: list[str] = []
    for idx, art in enumerate(articles, start=1):
        title = art.get("title", "")
        link = art.get("link", "")
        desc = art.get("desc", "")
        content = art.get("content", "")

        short_content = content[:2000]  # 너무 길어지는 것 방지

        blocks.append(
            f"[기사 {idx}]\n"
            f"제목: {title}\n"
            f"링크: {link}\n"
            f"요약: {desc}\n"
            f"본문 발췌:\n{short_content}\n"
        )

    context = "\n\n".join(blocks) if blocks else "관련 뉴스 기사를 찾지 못했습니다."

    prompt = f"""당신은 한국어 뉴스 기반 팩트체크 전문가입니다.

    검증할 주장:
    \"\"\"{claim}\"\"\"


    참고할 기사들:
    {context}

    아래 형식을 최대한 지켜서 한국어로 답변하세요.

    1. 사실 여부를 첫 줄에 명시 (세 가지 중 하나로 시작할 것):
    - "판단: 사실로 보입니다."
    - "판단: 거짓일 가능성이 높습니다."
    - "판단: 아직 불확실합니다."

    2. 근거:
    - 어떤 기사(번호 기준)에서 어떤 내용을 근거로 삼았는지 설명

    3. 논리적/정보적 허점:
    - 기사들 사이의 모순점
    - 아직 확인되지 않은 추측, 루머, 클릭베이트 표현

    4. 종합 코멘트:
    - 사용자가 이 이슈를 어떻게 받아들이면 좋을지, 주의할 점

    너무 장황하게 쓰지 말고, 핵심만 정리하세요."""

    return generate_with_gemini(prompt)


def main() -> None:
    # 간단 테스트용
    test_claim = "손흥민이 이번 시즌을 끝으로 은퇴한다는 소문이 사실인가?"
    print("=== Prompt Claim ===")
    print(test_claim)
    print("\n=== Gemini Response ===")
    answer = fact_check_claim(test_claim, [])
    print(answer)


if __name__ == "__main__":
    main()
