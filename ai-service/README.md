# DevSecWatch AI Service

AI-powered analysis service using Groq API.

## Setup

1. Create virtual environment: `python -m venv venv`
2. Activate: `source venv/bin/activate` or `venv\Scripts\activate`
3. Install: `pip install -r requirements.txt`
4. Set `.env` with `GROQ_API_KEY`.
5. Run: `uvicorn app.main:app --reload`
