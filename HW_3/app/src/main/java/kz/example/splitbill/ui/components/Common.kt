package kz.example.splitbill.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kz.example.splitbill.model.Friend
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ru = Locale("ru", "RU")

fun tenge(value: Long): String = "${NumberFormat.getNumberInstance(ru).format(value)} ₸"

fun formatDate(millis: Long): String = SimpleDateFormat("d MMMM, HH:mm", ru).format(Date(millis))

fun Friend.displayName(): String = if (isMe) "$name (вы)" else name

private val avatarColors = listOf(
    Color(0xFFFFD8E4), Color(0xFFD7F5E3), Color(0xFFDCE9FF), Color(0xFFFFE8C7), Color(0xFFE9DDFF), Color(0xFFD1F2F5)
)

@Composable
fun Avatar(friend: Friend, size: Dp = 40.dp) {
    Surface(shape = CircleShape, color = avatarColors[(friend.id % avatarColors.size).toInt()]) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Text(friend.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFF1F2933))
        }
    }
}

@Composable
fun SectionHeading(title: String, note: String = "", modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun MoneyRow(label: String, value: Long, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else null)
        Text(tenge(value), fontWeight = if (bold) FontWeight.Bold else null)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
}
