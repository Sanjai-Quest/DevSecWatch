from pydantic import BaseModel

class AnalysisRequest(BaseModel):
    vulnerability_type: str
    code_snippet: str
    file_path: str
    line_number: int

class AnalysisResponse(BaseModel):
    description: str
    fix_suggestion: str
    confidence: str  # "AI_GENERATED" or "TEMPLATE"
    is_template: bool = False

class HealthResponse(BaseModel):
    status: str
    version: str
    llm_service: str

class ChatRequest(BaseModel):
    message: str
    history: list = []
    context: str = ""

class ChatResponse(BaseModel):
    response: str
    model: str = "llama-3.3-70b-versatile"
