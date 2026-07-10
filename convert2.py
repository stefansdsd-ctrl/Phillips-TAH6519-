import urllib.request
import json
import os

api_key = os.environ.get("GEMINI_API_KEY")
url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key={api_key}"

java_code = open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.java").read()

prompt = "Convert this CFR decompiled Java code to its original Kotlin source code. Return ONLY the Kotlin code, no markdown wrappers."

data = {
    "contents": [{
        "parts": [
            {"text": prompt + "\n\n" + java_code}
        ]
    }]
}

try:
    req = urllib.request.Request(url, data=json.dumps(data).encode("utf-8"), headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as response:
        result = json.loads(response.read().decode("utf-8"))
        text = result["candidates"][0]["content"]["parts"][0]["text"]
        
        # Remove markdown if present
        if text.startswith("```kotlin"):
            text = text[9:]
        if text.startswith("```"):
            text = text[3:]
        if text.endswith("```"):
            text = text[:-3]
            
        with open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt", "w") as f:
            f.write(text.strip())
    print("Conversion complete!")
except Exception as e:
    import traceback
    traceback.print_exc()
