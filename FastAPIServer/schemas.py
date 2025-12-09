from pydantic import BaseModel

class SignupRequest(BaseModel):
    username: str
    password: str
    phone: str
    nickname: str

class SignupResponse(BaseModel):
    id: int
    username: str
    phone: str
    nickname: str

class UserExistsResponse(BaseModel):
    exists: bool
    nickname: str | None = None

class LoginRequest(BaseModel):
    username: str
    password: str

class LoginResponse(BaseModel):
    id: int
    username: str
    phone: str
    nickname: str

class Article(BaseModel):
    title: str
    link: str
    desc: str
    content: str

class FactCheckRequest(BaseModel):
    claim: str

class FactCheckResponse(BaseModel):
    claim: str
    verdict: str   # "true" | "false" | "uncertain" | "unknown"
    analysis: str  # Gemini가 정리한 설명 텍스트
    articles: list[Article]

class AnalyzeRequest(BaseModel):
    text: str

class AnalyzeResponse(BaseModel):
    analysis: str | None = None