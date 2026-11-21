package kz.example.splitbill.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val light = lightColorScheme(
    primary = Color(0xFF325D9A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9FF),
    secondaryContainer = Color(0xFFE4EAF2),
    tertiary = Color(0xFF2E7D4F),
    tertiaryContainer = Color(0xFFD5F2DF),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFF7F8FC),
    surfaceContainerLow = Color.White,
    surfaceVariant = Color(0xFFECF0F7)
)

private val dark = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF00315F),
    primaryContainer = Color(0xFF1B4677),
    secondaryContainer = Color(0xFF3A4350),
    tertiary = Color(0xFF8FD8A9),
    tertiaryContainer = Color(0xFF1C4D30)
)

// Colors for money that must be received / paid.
val Positive @Composable get() = MaterialTheme.colorScheme.tertiary
val Negative @Composable get() = MaterialTheme.colorScheme.error

@Composable
fun SplitBillTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) dark else light, content = content)
}
