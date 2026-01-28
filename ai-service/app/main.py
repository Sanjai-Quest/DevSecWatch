from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import logging
import uvicorn
from app.models import AnalysisRequest, AnalysisResponse, HealthResponse, ChatRequest, ChatResponse
from app.services.llm_service import LLMService
from app.services.validation_service import ValidationService
from app.templates.explanations import get_template
from app.config import settings

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="DevSecWatch AI Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[settings.backend_url, "*"],  
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)

# Initialize services
llm_service = LLMService()
validation_service = ValidationService()

@app.get("/health", response_model=HealthResponse)
async def health():
    return HealthResponse(
        status="healthy",
        version="1.0.0",
        llm_service="groq" if llm_service.client else "template_only"
    )

@app.post("/analyze", response_model=AnalysisResponse)
async def analyze_vulnerability(request: AnalysisRequest):
    logger.info(f"Analyzing {request.vulnerability_type} in {request.file_path}:{request.line_number}")
    
    try:
        # Try AI generation
        ai_result = await llm_service.generate_explanation(
            request.vulnerability_type,
            request.code_snippet,
            request.file_path,
            request.line_number
        )
        
        # Validate AI response
        if ai_result and validation_service.validate_explanation(
            request.vulnerability_type,
            ai_result["description"],
            request.code_snippet
        ):
            logger.info("Using AI-generated explanation")
            return AnalysisResponse(
                description=ai_result["description"],
                fix_suggestion=ai_result["fix"],
                confidence="AI_GENERATED",
                is_template=False
            )
        
        # Fallback to template
        logger.info("Using template explanation (Fallback)")
        template = get_template(request.vulnerability_type)
        return AnalysisResponse(
            description=template["description"],
            fix_suggestion=template["fix"],
            confidence="TEMPLATE",
            is_template=True
        )
        
    except Exception as e:
        logger.error(f"Analysis failed: {e}")
        template = get_template(request.vulnerability_type)
        return AnalysisResponse(
            description=template["description"],
            fix_suggestion=template["fix"],
            confidence="TEMPLATE",
            is_template=True
        )

@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    logger.info(f"Chat request received: {request.message[:50]}...")
    
    try:
        if not llm_service.client:
            return ChatResponse(
                response="I apologize, but the AI service is currently unavailable. Please try again later.",
                model="template"
            )
        
        # Build conversation messages
        messages = []
        
        # Add system context if provided
        if request.context:
            messages.append({"role": "system", "content": request.context})
        
        # Add conversation history
        for msg in request.history[-10:]:  # Last 10 messages
            messages.append({"role": msg.get("role", "user"), "content": msg.get("content", "")})
        
        # Add current user message
        messages.append({"role": "user", "content": request.message})
        
        # Call Groq API
        response = llm_service.client.chat.completions.create(
            model=llm_service.model,
            messages=messages,
            temperature=0.7,
            max_tokens=1000,
            timeout=15
        )
        
        ai_response = response.choices[0].message.content
        logger.info("Chat response generated successfully")
        
        return ChatResponse(
            response=ai_response,
            model=llm_service.model
        )
        
    except Exception as e:
        logger.error(f"Chat failed: {e}")
        return ChatResponse(
            response="I apologize, but I encountered an error. Please try rephrasing your question.",
            model="error"
        )

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
