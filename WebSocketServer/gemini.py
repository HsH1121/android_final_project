from google import genai

GEMINI_API_KEY = ""

MODEL_NAME = "gemini-2.5-flash"

def generate_with_gemini(prompt: str) -> str:
    client = genai.Client(api_key=GEMINI_API_KEY)

    response = client.models.generate_content(
        model=MODEL_NAME,
        contents=prompt,
    )

    return response.text

def main():
    prompt = ""

    print("=== Prompt ===")
    print(prompt)
    print("\n=== Gemini Response ===")
    answer = generate_with_gemini(prompt)
    print(answer)

if __name__ == "__main__":
    main()
