# TalentMatch AI - Python FastAPI Service Launcher
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$env:USE_TF = "0"
$env:TF_ENABLE_ONEDNN_OPTS = "0"
$env:HF_HUB_OFFLINE = "1"
$env:TRANSFORMERS_OFFLINE = "1"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Starting TalentMatch AI - Python Microservice (Port 8000) " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

& ".\.venv\Scripts\python.exe" -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
