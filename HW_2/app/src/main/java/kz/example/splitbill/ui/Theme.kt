package kz.example.splitbill.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val palette = lightColorScheme(
    primary = Color(0xFF325D9A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9FF),
    secondaryContainer = Color(0xFFE4EAF2),
    background = Color(0xFFF7F8FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFECF0F7)
)

@Composable
fun SplitBillTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = palette, content = content)
}
