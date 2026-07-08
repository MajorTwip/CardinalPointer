package com.cardinalpointer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Deliberately single-theme: a field control console for a mast-mounted
 * camera rig reads as an instrument panel, not a document — it commits to
 * one dark operating environment the way a cockpit or radar scope does.
 */
object ConsoleColors {
    val Bg = Color(0xFF0B0F14)
    val Panel = Color(0xFF131A21)
    val PanelRaised = Color(0xFF1A232B)
    val Line = Color(0xFF26323C)
    val LineSoft = Color(0xFF1C262E)
    val Ink = Color(0xFFCBD7DD)
    val InkDim = Color(0xFF7D8B95)
    val InkFaint = Color(0xFF4C5860)
    val Accent = Color(0xFF49E6D2)
    val AccentInk = Color(0xFF04211D)
    val Amber = Color(0xFFFFB02C)
    val Red = Color(0xFFFF5F5A)
}

private val ConsoleColorScheme = darkColorScheme(
    primary = ConsoleColors.Accent,
    onPrimary = ConsoleColors.AccentInk,
    background = ConsoleColors.Bg,
    onBackground = ConsoleColors.Ink,
    surface = ConsoleColors.Panel,
    onSurface = ConsoleColors.Ink,
    error = ConsoleColors.Red
)

@Composable
fun ConsoleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ConsoleColorScheme, content = content)
}
