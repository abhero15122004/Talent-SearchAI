@echo off
echo ===================================================
echo Starting TalentMatch AI - Python NLP & Matching Service
echo ===================================================
cd /d "%~dp0"
set "USE_TF=0"
set "TF_ENABLE_ONEDNN_OPTS=0"
set "HF_HUB_OFFLINE=1"
set "TRANSFORMERS_OFFLINE=1"
".venv\Scripts\python.exe" -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
pause
