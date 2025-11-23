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
