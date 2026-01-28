import logging
import re
from groq import Groq
from app.config import settings

logger = logging.getLogger(__name__)

class LLMService:
    def __init__(self):
        # We initialize the client lazy or here. 
        # CAUTION: If api_key is dummy, client might fail on init or on first call.
        # Groq client checks for api_key existence.
        self.client = None
        if settings.groq_api_key and settings.groq_api_key != "dummy_key":
            try:
                self.client = Groq(api_key=settings.groq_api_key)
            except Exception as e:
                logger.warning(f"Failed to initialize Groq client: {e}")
        
        self.model = "llama-3.3-70b-versatile"

    async def generate_explanation(
        self,
        vulnerability_type: str,
        code_snippet: str,
        file_path: str,
        line_number: int
    ) -> dict | None:
        if not self.client:
            logger.warning("Groq client not initialized. Using template fallback.")
            return None

        prompt = self._build_prompt(vulnerability_type, code_snippet, file_path, line_number)
        
        try:
            # Groq Python client is synchronous by default unless using AsyncGroq? 
            # The prompt in Step 411 used `self.client.chat.completions.create`. 
            # In a FastAPI async route, blocking calls can block the event loop.
            # Ideally we run this in threadpool or use AsyncGroq. 
            # For simplicity matching the plan, we'll keep it simple but acknowledge blocking.
            # Better: Use AsyncGroq if available or run_in_executor.
            # Let's stick to the plan's synchronous call for now, but wrapped if needed?
            # Actually, standard Groq client is sync. 
            
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[{"role": "user", "content": prompt}],
                temperature=0.3,
                max_tokens=400,
                timeout=10
            )
            
            content = response.choices[0].message.content
            logger.info(f"Raw Groq Response: {content}")
            return self._parse_response(content)
            
        except Exception as e:
            logger.error(f"LLM API failed: {e}")
            return None

    def _build_prompt(self, vuln_type: str, code: str, file: str, line: int) -> str:
        vuln_display = vuln_type.replace('_', ' ')
        return f"""You are a security expert explaining a vulnerability to a junior developer.
Vulnerability Type: {vuln_type}
File: {file}
Line: {line}
Code:
```
{code}
```

Provide:
1. A clear explanation of why this is a security risk (2-3 sentences)
2. A specific fix for THIS code (not generic advice)

Requirements:
- Explicitly mention "{vuln_display}"
- Reference actual variable/method names from the code above
- Be concrete and actionable
- Maximum 200 words total

Format your response EXACTLY as follows:
DESCRIPTION: <your explanation>
FIX: <your specific fix>
"""
    
    def _parse_response(self, text: str) -> dict:
        desc_match = re.search(r'DESCRIPTION:\s*(.+?)(?=FIX:|$)', text, re.DOTALL)
        fix_match = re.search(r'FIX:\s*(.+)', text, re.DOTALL)
        
        description = desc_match.group(1).strip() if desc_match else text[:300]
        fix = fix_match.group(1).strip() if fix_match else "Review code manually for security issues."
        
        return {
            "description": description,
            "fix": fix
        }
