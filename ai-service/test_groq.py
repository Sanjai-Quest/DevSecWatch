import os
from groq import Groq
from dotenv import load_dotenv

load_dotenv()

api_key = os.getenv("GROQ_API_KEY")
print(f"Checking API Key: {api_key[:5]}...****************" if api_key else "Checking API Key: None")

try:
    client = Groq(api_key=api_key)
    # Try the new model
    model = "llama-3.3-70b-versatile" 
    print(f"Testing model: {model}")
    
    chat_completion = client.chat.completions.create(
        messages=[
            {
                "role": "user",
                "content": "Explain SQL Injection in one sentence.",
            }
        ],
        model=model,
    )

    print("\nSUCCESS! Groq API Responded:")
    print(chat_completion.choices[0].message.content)

except Exception as e:
    print(f"\nFAILURE. Error details:\n{e}")
