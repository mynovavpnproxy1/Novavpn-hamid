package com.novavpn.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NovaDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DB6FF),
    onPrimary = Color.White,
    secondary = Color(0xFF8EC5FF),
    tertiary = Color(0xFF7DD3FC),
    background = Color(0xFF061722),
    surface = Color(0xFF0F2338),
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFDB4C4C),
    onError = Color.White
)

private val NovaLightColorScheme = lightColorScheme(
    primary = Color(0xFF4DB6FF),
    onPrimary = Color.White,
    secondary = Color(0xFF8EC5FF),
    tertiary = Color(0xFF7DD3FC),
    background = Color(0xFFF2F8FF),
    surface = Color.White,
    onBackground = Color(0xFF081C2B),
    onSurface = Color(0xFF081C2B),
    error = Color(0xFFDB4C4C),
    onError = Color.White
)

@Composable
fun NovaVPNTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NovaDarkColorScheme else NovaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
