from sqlalchemy import Column, Integer, String, DateTime, func

from db import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, nullable=False, index=True)
    phone = Column(String(20), unique=True, nullable=False, index=True)
    password = Column(String(128), nullable=False)  # 지금은 평문, 나중에 해시로 변경
    nickname = Column(String(50), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())