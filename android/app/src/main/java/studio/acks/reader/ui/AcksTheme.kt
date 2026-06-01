package studio.acks.reader.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Shell color tokens (dark-only app chrome) ────────────────────────────────
val AcksBg       = Color(0xFF0B0B0C)
val AcksSurface  = Color(0xFF151517)
val AcksSurface2 = Color(0xFF1C1C1F)
val AcksBorder   = Color(0xFF28282C)
val AcksBorder2  = Color(0xFF34343A)
val AcksFg       = Color(0xFFF5F5F6)
val AcksFg2      = Color(0xFF9A9AA0)
val AcksFg3      = Color(0xFF5E5E64)
val AcksAccent   = Color(0xFFF26419)
val AcksAccentSoft = Color(0x22F26419)

private val ColorScheme = darkColorScheme(
    primary            = AcksAccent,
    onPrimary          = Color.White,
    primaryContainer   = AcksAccentSoft,
    background         = AcksBg,
    surface            = AcksSurface,
    surfaceVariant     = AcksSurface2,
    onBackground       = AcksFg,
    onSurface          = AcksFg,
    onSurfaceVariant   = AcksFg2,
    outline            = AcksBorder,
    outlineVariant     = AcksBorder2,
)

@Composable
fun AcksTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, content = content)
}
