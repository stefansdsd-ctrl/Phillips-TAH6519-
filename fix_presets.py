content = open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt").read()
if "val presets" not in content:
    content = content.replace("class HeadphoneViewModel", "class HeadphoneViewModel")
    content = content.replace("}", "    val presets = kotlinx.coroutines.flow.MutableStateFlow<Map<String, List<Float>>>(emptyMap())\n}")
    with open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt", "w") as f:
        f.write(content)
