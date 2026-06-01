package studio.acks.reader

data class ThemeMeta(
    val id: String,
    val name: String,
    val en: String,
    val tag: String,
    val modes: List<String>,
    val defaultMode: String,
    val swatchLight: Long,   // background color
    val swatchDark: Long,
    val accentLight: Long,   // accent / heading color (for thumbnail)
    val accentDark: Long,
    val titleLight: Long,    // title text color
    val titleDark: Long
)

object AcksThemes {
    val all = listOf(
        ThemeMeta("clean",      "简洁阅读",  "Clean",       "阅读", listOf("light","dark"), "light",
            0xFFF8F6F2, 0xFF16140F, 0xFFC2613A, 0xFFE08A4E, 0xFF23211D, 0xFFF1EDE3),
        ThemeMeta("business",   "商业报告",  "Business",    "商务", listOf("light","dark"), "light",
            0xFFFFFFFF, 0xFF0D1117, 0xFF1F5FA8, 0xFF4D8FD6, 0xFF0F1C2E, 0xFFEEF2F7),
        ThemeMeta("technical",  "技术文档",  "Technical",   "技术", listOf("light","dark"), "light",
            0xFFF5F7FA, 0xFF161B22, 0xFF0E9F6E, 0xFF3DDAA6, 0xFF1A2332, 0xFFE6EDF3),
        ThemeMeta("darkcode",   "深色代码",  "Dark Code",   "代码", listOf("dark"),         "dark",
            0xFF0D1117, 0xFF0D1117, 0xFFF97316, 0xFFF97316, 0xFFDDE6F0, 0xFFDDE6F0),
        ThemeMeta("social",     "社交长图",  "Social",      "社交", listOf("light","dark"), "light",
            0xFFFFFFFF, 0xFF111113, 0xFFF26419, 0xFFF26419, 0xFF1A1A1C, 0xFFF5F5F6),
        ThemeMeta("academic",   "学术论文",  "Academic",    "学术", listOf("light"),        "light",
            0xFFFAF8F0, 0xFFFAF8F0, 0xFF7A1F2B, 0xFF7A1F2B, 0xFF1A1A1A, 0xFF1A1A1A),
        ThemeMeta("wechat",     "微信图文",  "WeChat",      "社交", listOf("light"),        "light",
            0xFFFFFFFF, 0xFFFFFFFF, 0xFF07A35A, 0xFF07A35A, 0xFF1A1A1A, 0xFF1A1A1A),
        ThemeMeta("magazine",   "杂志美文",  "Magazine",    "杂志", listOf("light","dark"), "light",
            0xFFFCFAF7, 0xFF1C1917, 0xFFB33A2B, 0xFFD05040, 0xFF1A1310, 0xFFE8E0D5),
        ThemeMeta("aireport",   "AI 报告",   "AI Report",   "商务", listOf("light","dark"), "dark",
            0xFFF0F4FF, 0xFF0D0F1A, 0xFF7C5CFF, 0xFF9B85FF, 0xFF0F1240, 0xFFE8E5FF),
        ThemeMeta("euro",       "欧式极简",  "Euro",        "极简", listOf("light","dark"), "light",
            0xFFFFFFFF, 0xFF111111, 0xFF9C7A3C, 0xFFBFA060, 0xFF111111, 0xFFEEEEEE),
        ThemeMeta("cnclassic",  "国风古典",  "CN Classic",  "国风", listOf("light"),        "light",
            0xFFFBF6EC, 0xFFFBF6EC, 0xFF9E2B25, 0xFF9E2B25, 0xFF1A1209, 0xFF1A1209),
        ThemeMeta("cnvertical", "竖排古风",  "CN Vertical", "国风", listOf("light"),        "light",
            0xFFFAF3E0, 0xFFFAF3E0, 0xFF8C2820, 0xFF8C2820, 0xFF1A1209, 0xFF1A1209),
        ThemeMeta("poster",     "海报封面",  "Poster",      "海报", listOf("light","dark"), "dark",
            0xFF0A0A0A, 0xFF0A0A0A, 0xFFFF2D00, 0xFFFF4D00, 0xFFFFFFFF, 0xFFFFFFFF),
        ThemeMeta("cyberpunk",  "赛博朋克",  "Cyberpunk",   "Dark",   listOf("dark"),         "dark",
            0xFF050911, 0xFF050911, 0xFF00F0C0, 0xFF00F0C0, 0xFFC8E4FF, 0xFFC8E4FF),
        ThemeMeta("retro",      "复古打字机","Retro Typewriter","Retro",listOf("light","dark"),"light",
            0xFFF5EDD4, 0xFF140D06, 0xFF8B4020, 0xFFD4834A, 0xFF2A1A0A, 0xFFE8D9BC),
        ThemeMeta("pastel",     "粉彩少女",  "Pastel Pink", "Soft",   listOf("light"),        "light",
            0xFFFEF6F9, 0xFFFEF6F9, 0xFFD64E8A, 0xFFD64E8A, 0xFF2E1020, 0xFF2E1020),
        ThemeMeta("finance",    "财经数据",  "Finance",     "Finance",listOf("light","dark"), "dark",
            0xFFFAFBFE, 0xFF060B16, 0xFF9B7A12, 0xFFE8BE48, 0xFF0C1830, 0xFFC8D8EC),
        ThemeMeta("newspaper",  "经典报纸",  "Newspaper",   "Classic",listOf("light"),        "light",
            0xFFFFFEF5, 0xFFFFFEF5, 0xFF0A0A08, 0xFF0A0A08, 0xFF0A0A08, 0xFF0A0A08),
        ThemeMeta("darkpurple", "暗夜紫",    "Dark Purple", "Night",  listOf("dark"),         "dark",
            0xFF100C1C, 0xFF100C1C, 0xFF9B7EE8, 0xFF9B7EE8, 0xFFD4C8F0, 0xFFD4C8F0),
        ThemeMeta("nature",     "自然有机",  "Nature",      "Green",  listOf("light","dark"), "light",
            0xFFF4F7EE, 0xFF0C140A, 0xFF3D7020, 0xFF6AB440, 0xFF1C2E0C, 0xFFC8E0B0),
        ThemeMeta("clinical",   "医学临床",  "Clinical",    "Medical",listOf("light"),        "light",
            0xFFF7FAFF, 0xFFF7FAFF, 0xFF005AB4, 0xFF005AB4, 0xFF0A1630, 0xFF0A1630),
        ThemeMeta("festival",   "节日喜庆",  "Festival",    "Bold",   listOf("light","dark"), "light",
            0xFFFFF8F0, 0xFF140606, 0xFFCC2020, 0xFFE84040, 0xFF3A0A0A, 0xFFFFE8D0),
        ThemeMeta("mono",       "极简单色",  "Monochrome",  "Minimal",listOf("light","dark"), "light",
            0xFFFFFFFF, 0xFF111111, 0xFF0A0A0A, 0xFFE8E8E8, 0xFF0A0A0A, 0xFFE8E8E8),
        ThemeMeta("gradient",   "渐变现代",  "Gradient",    "Modern", listOf("light"),        "light",
            0xFFFFFFFF, 0xFFFFFFFF, 0xFF7C3AED, 0xFF7C3AED, 0xFF1A1A2E, 0xFF1A1A2E),
    )

    fun find(id: String): ThemeMeta? = all.find { it.id == id }

    fun bgFor(meta: ThemeMeta, mode: String): Long =
        if (mode == "dark") meta.swatchDark else meta.swatchLight

    fun accentFor(meta: ThemeMeta, mode: String): Long =
        if (mode == "dark") meta.accentDark else meta.accentLight

    fun titleFor(meta: ThemeMeta, mode: String): Long =
        if (mode == "dark") meta.titleDark else meta.titleLight
}
