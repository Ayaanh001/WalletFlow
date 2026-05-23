package com.hussain.walletflow.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFD0BCFF),
        background = Color(0xFF1C1B1F),
        surface = Color(0xFF1C1B1F),
        secondary = PurpleGrey80,
        tertiary = Pink80
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF6750A4),
        background = Color(0xFFFFFBFE),
        surface = Color(0xFFFFFBFE),
        secondary = PurpleGrey40,
        tertiary = Pink40
    )

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }

    TransactionTrackerTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}

@Composable
fun TransactionTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme =
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                }
                darkTheme -> DarkColorScheme
                else -> LightColorScheme
            }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val backgroundColor = colorScheme.background.toArgb()
            
            // Sync window background with theme to prevent flash during transitions
            window.setBackgroundDrawable(ColorDrawable(backgroundColor))

            // Use transparent status bar for smoother edge-to-edge transitions
            window.statusBarColor = Color.Transparent.toArgb()
            // Match the navigation bar with our custom bottom bar color
            window.navigationBarColor = colorScheme.surfaceContainer.toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            // Light status bar icons in dark theme, dark icons in light theme
            insetsController.isAppearanceLightStatusBars = !darkTheme
            // Light navigation bar icons in dark theme, dark icons in light theme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
