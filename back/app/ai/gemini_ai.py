import google.generativeai as genai
import os

class GeminiAI:
    def __init__(self, api_key):
        genai.configure(api_key=api_key)
        # 가장 안정적인 텍스트 모델 사용
        self.model = genai.GenerativeModel("models/gemini-flash-latest")

    def analyze_text(self, prompt):
        try:
            response = self.model.generate_content(prompt)
            return response.text
        except Exception as e:
            print("Gemini Error:", e)
            return None
if __name__ == "__main__":
    from dotenv import load_dotenv
    load_dotenv()
    key = os.getenv("GEMINI_API_KEY")
    print("KEY:", repr(key))   # 숨은 문자 확인용
    genai.configure(api_key=key)
    for m in genai.list_models():
        print(m.name)

'''
import google.generativeai as genai

class GeminiAI:
    def __init__(self, api_key):
        genai.configure(api_key=api_key)
        # 속도+정확도 균형 좋은 모델
        self.model = genai.GenerativeModel("models/gemini-flash-latest")

    def analyze_text(self, user_text):
        try:
            # 고급 텍스트 분석 프롬프트
            prompt = f"""
다음 문장을 분석해줘:

\"\"\"{user_text}\"\"\"

아래 기준에 따라 상세하게 분석 보고서를 작성해줘:

1. 논리적 오류(Logical fallacies)
   - 감정 호소, 성급한 일반화, 거짓 원인, 허수아비 공격, 순환 논증 등  

2. 근거가 부족한 주장(Unsubstantiated claims) 
   - '아마', '분명', '누구나 알고 있다' 같은 표현  
   - 근거가 없는 단정적 표현  
   
3. **사실관계 확인이 필요한 부분(Factual verification needed)**  
   - 사건, 통계, 지명, 인물, 날짜 등 검증이 필요한 정보  
   - 사실 여부가 불명확한 문장을 지적해줘.

최종 출력 형식:
- 논리적 오류: …
- 근거 부족 주장: …
- 사실관계 검증 필요 문장: …
- 종합 평가: …
"""
            response = self.model.generate_content(prompt)
            return response.text

        except Exception as e:
            print("Gemini Error:", e)
            return None
'''