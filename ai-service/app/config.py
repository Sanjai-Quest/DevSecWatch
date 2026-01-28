from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    groq_api_key: str = "dummy_key"
    redis_url: str = "redis://localhost:6379"
    backend_url: str = "http://localhost:8080"
    
    class Config:
        env_file = ".env"
        # Ignore extra fields in .env
        extra = "ignore"

settings = Settings()
