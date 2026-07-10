import urllib.request
import json
import os

api_key = os.environ.get("GEMINI_API_KEY")
url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key={api_key}"

java_code = open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.java").read()

prompt = "You are an expert Kotlin developer. Convert this CFR decompiled Java code back to its original Kotlin source code. This is an Android ViewModel class named HeadphoneViewModel. The original code was written in Kotlin using Coroutines, StateFlow, and Android MediaPlayer. Please do your best to reconstruct the idiomatic Kotlin code (e.g. use properties instead of getters, use viewModelScope.launch instead of BuildersKt.launch, etc.). Return ONLY the raw Kotlin code, no markdown block wrappers."

data = {
    "contents": [{
        "parts": [
            {"text": prompt + "\n\n" + java_code}
        ]
    }]
}

req = urllib.request.Request(url, data=json.dumps(data).encode("utf-8"), headers={"Content-Type": "application/json"})
with urllib.request.urlopen(req) as response:
    result = json.loads(response.read().decode("utf-8"))
    text = result["candidates"][0]["content"]["parts"][0]["text"]
    with open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt", "w") as f:
        f.write(text)
print("Conversion complete!")
