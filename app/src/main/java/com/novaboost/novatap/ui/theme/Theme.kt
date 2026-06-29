package com.novaboost.novatap.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyberBlue,
    secondary = ElectricPurple,
    tertiary = GlowGreen,
    background = SpaceDarkBg,
    surface = CardDarkBg,
    onPrimary = SpaceDarkBg,
    onSecondary = SpaceDarkBg,
    onBackground = LightCyan,
    onSurface = LightCyan,
    error = AlertRed
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = Purple40,
    tertiary = GlowGreen,
    background = LightBg,
    surface = LightCyan,
    onPrimary = LightCyan,
    onSecondary = LightCyan,
    onBackground = SpaceDarkBg,
    onSurface = SpaceDarkBg,
    error = AlertRed
)

@Composable
fun NovaTapTheme(
    darkTheme: Boolean = true, // Default to dark theme for the app experience
    dynamicColor: Boolean = false, // Set false to preserve our custom brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var currentContext = view.context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is Activity) {
                    val window = currentContext.window
                    window.statusBarColor = colorScheme.background.toArgb()
                    window.navigationBarColor = colorScheme.background.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                    break
                }
                currentContext = currentContext.baseContext
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
