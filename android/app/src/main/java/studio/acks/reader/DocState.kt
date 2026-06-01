package studio.acks.reader

enum class Format { MARKDOWN, HTML, UNSUPPORTED }

enum class Lifecycle { LOADING, RENDERING, RENDERED, ERROR, LARGE, UNSUPPORTED, MISSING }

data class Overrides(
    val fonts: Map<String, String> = emptyMap(),
    val baseSize: Int? = null,
    val colors: Map<String, String> = emptyMap()
) {
    fun toJson(): String {
        val sb = StringBuilder("{")
        if (fonts.isNotEmpty()) sb.append(""""fonts":${mapJson(fonts)},""")
        baseSize?.let { sb.append(""""baseSize":$it,""") }
        if (colors.isNotEmpty()) sb.append(""""colors":${mapJson(colors)},""")
        if (sb.last() == ',') sb.deleteCharAt(sb.length - 1)
        return sb.append("}").toString()
    }

    private fun mapJson(m: Map<String, String>) =
        m.entries.joinToString(",", "{", "}") { """"${it.key}":${jsonStr(it.value)}""" }
}

data class DocState(
    val id: String,
    val title: String,
    val format: Format,
    val sandboxPath: String = "",
    val markdownSource: String = "",
    val themeId: String = "aireport",
    val mode: String = "dark",
    val viewport: String = "phone",
    val customWidth: Int = 600,
    val htmlMode: String = "safe",
    val overrides: Overrides = Overrides(),
    val lifecycle: Lifecycle = Lifecycle.LOADING,
    val fontSource: String = FontManager.LOCAL,
    val fontScale: Float = 1.0f
) {
    /** Physical viewport width in CSS pixels; null = device width (phone). */
    val viewportWidthPx: Int? get() = when (viewport) {
        "phone"   -> null
        "desktop" -> 1024
        "a4"      -> 794
        "social"  -> 480
        "custom"  -> customWidth
        else      -> null
    }

    /** JSON options consumed by ACKS.render() in host.html */
    fun renderOptsJson(): String {
        val interactive = format != Format.HTML || htmlMode == "interactive"
        val effectiveOv = if (fontScale != 1.0f) {
            overrides.copy(baseSize = (16 * fontScale).toInt())
        } else overrides
        val sb = StringBuilder()
        sb.append("""{"themeId":${jsonStr(themeId)},"mode":${jsonStr(mode)},""")
        sb.append(""""interactive":$interactive,""")
        sb.append(""""fontSource":${jsonStr(fontSource)},""")
        viewportWidthPx?.let { sb.append(""""viewportWidth":$it,""") }
        sb.append(""""ov":${effectiveOv.toJson()}}""")
        return sb.toString()
    }
}

data class AcksMessage(val kind: String, val raw: String)

internal fun jsonStr(s: String): String = buildString {
    append('"')
    for (c in s) when (c) {
        '\\' -> append("\\\\")
        '"'  -> append("\\\"")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        else -> append(c)
    }
    append('"')
}
