package studio.acks.reader.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Dark shell tokens ────────────────────────────────────────────────────────
val AcksBgDark       = Color(0xFF0B0B0C)
val AcksSurfaceDark  = Color(0xFF151517)
val AcksSurface2Dark = Color(0xFF1C1C1F)
val AcksBorderDark   = Color(0xFF28282C)
val AcksBorder2Dark  = Color(0xFF34343A)
val AcksFgDark       = Color(0xFFF5F5F6)
val AcksFg2Dark      = Color(0xFF9A9AA0)
val AcksFg3Dark      = Color(0xFF5E5E64)

// ── Light shell tokens ───────────────────────────────────────────────────────
val AcksBgLight       = Color(0xFFF2F2F4)
val AcksSurfaceLight  = Color(0xFFFFFFFF)
val AcksSurface2Light = Color(0xFFEEEEF1)
val AcksBorderLight   = Color(0xFFDFDFE4)
val AcksBorder2Light  = Color(0xFFCECED5)
val AcksFgLight       = Color(0xFF1A1A1C)
val AcksFg2Light      = Color(0xFF5A5A64)
val AcksFg3Light      = Color(0xFF9A9AA6)

// ── Accent (same in both modes) ───────────────────────────────────────────────
val AcksAccent     = Color(0xFFF26419)
val AcksAccentSoft = Color(0x22F26419)

// ── Runtime tokens (set by AcksTheme, read everywhere) ────────────────────────
var AcksBg       = AcksBgDark
var AcksSurface  = AcksSurfaceDark
var AcksSurface2 = AcksSurface2Dark
var AcksBorder   = AcksBorderDark
var AcksBorder2  = AcksBorder2Dark
var AcksFg       = AcksFgDark
var AcksFg2      = AcksFg2Dark
var AcksFg3      = AcksFg3Dark

private val DarkColorScheme = darkColorScheme(
    primary            = AcksAccent,
    onPrimary          = Color.White,
    primaryContainer   = AcksAccentSoft,
    background         = AcksBgDark,
    surface            = AcksSurfaceDark,
    surfaceVariant     = AcksSurface2Dark,
    onBackground       = AcksFgDark,
    onSurface          = AcksFgDark,
    onSurfaceVariant   = AcksFg2Dark,
    outline            = AcksBorderDark,
    outlineVariant     = AcksBorder2Dark,
)

private val LightColorScheme = lightColorScheme(
    primary            = AcksAccent,
    onPrimary          = Color.White,
    primaryContainer   = AcksAccentSoft,
    background         = AcksBgLight,
    surface            = AcksSurfaceLight,
    surfaceVariant     = AcksSurface2Light,
    onBackground       = AcksFgLight,
    onSurface          = AcksFgLight,
    onSurfaceVariant   = AcksFg2Light,
    outline            = AcksBorderLight,
    outlineVariant     = AcksBorder2Light,
)

/** appTheme: "system" | "dark" | "light" */
@Composable
fun AcksTheme(appTheme: String = "dark", content: @Composable () -> Unit) {
    val dark = when (appTheme) {
        "light"  -> false
        "dark"   -> true
        else     -> isSystemInDarkTheme()  // "system"
    }
    // Update runtime tokens so all composables see the right colors
    AcksBg       = if (dark) AcksBgDark       else AcksBgLight
    AcksSurface  = if (dark) AcksSurfaceDark  else AcksSurfaceLight
    AcksSurface2 = if (dark) AcksSurface2Dark else AcksSurface2Light
    AcksBorder   = if (dark) AcksBorderDark   else AcksBorderLight
    AcksBorder2  = if (dark) AcksBorder2Dark  else AcksBorder2Light
    AcksFg       = if (dark) AcksFgDark       else AcksFgLight
    AcksFg2      = if (dark) AcksFg2Dark      else AcksFg2Light
    AcksFg3      = if (dark) AcksFg3Dark      else AcksFg3Light

    MaterialTheme(
        colorScheme = if (dark) DarkColorScheme else LightColorScheme,
        content     = content
    )
}
