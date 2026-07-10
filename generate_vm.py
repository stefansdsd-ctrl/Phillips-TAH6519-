import re

java_code = open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.java").read()

properties = []
methods = []

for line in java_code.split("\n"):
    line = line.strip()
    if line.startswith("private final StateFlow<"):
        # private final StateFlow<Boolean> mediaIsPlaying;
        m = re.match(r"private final StateFlow<(.+?)> (.+?);", line)
        if m:
            t, name = m.groups()
            properties.append(f"    val {name} = MutableStateFlow<{t}>(null as {t})")

# Let's just generate a minimal stub
with open("stub.kt", "w") as f:
    f.write("package com.example.ui\n")
    f.write("import androidx.lifecycle.ViewModel\n")
    f.write("import kotlinx.coroutines.flow.MutableStateFlow\n")
    f.write("class HeadphoneViewModel : ViewModel() {\n")
    for p in properties:
        f.write(p + "\n")
    f.write("}\n")
