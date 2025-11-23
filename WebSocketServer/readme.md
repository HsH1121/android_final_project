# 1) 프로젝트 폴더로 이동
cd "C:\Users\eju20\Desktop\University_Laptop\4-2\Android Programming\Projects\Final\workspace\WebSocketServer"

# 2) 가상환경 생성 (.venv 폴더 생성)
python -m venv .venv

# 3) 가상환경 활성화
.\.venv\Scripts\Activate.ps1
# 프롬프트 앞에 (.venv) 붙으면 성공

# 4) pip 최신으로 업데이트 (선택)
pip install --upgrade pip

# 5) requirements.txt에 적어둔 라이브러리 설치
pip install -r requirements.txt