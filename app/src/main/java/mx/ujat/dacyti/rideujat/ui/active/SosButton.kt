package mx.ujat.dacyti.rideujat.ui.active

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mx.ujat.dacyti.rideujat.data.model.SosContact
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SosLongPressButton(
    onSosActivated: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressing by remember { mutableStateOf(false) }
    var pressProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(pressing) {
        if (pressing) {
            val startTime = System.currentTimeMillis()
            while (pressing) {
                val elapsed = System.currentTimeMillis() - startTime
                pressProgress = (elapsed / 3000f).coerceIn(0f, 1f)
                if (pressProgress >= 1f) {
                    pressing = false
                    pressProgress = 0f
                    onSosActivated()
                    return@LaunchedEffect
                }
                delay(50)
            }
        }
        pressProgress = 0f
    }

    Box(
        modifier = modifier
            .size(72.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressing = true
                        tryAwaitRelease()
                        pressing = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color.Red)
        }

        if (pressing && pressProgress > 0f) {
            CircularProgressIndicator(
                progress = { pressProgress },
                modifier = Modifier.size(64.dp),
                color = Color.White.copy(alpha = 0.9f),
                trackColor = Color.Red.copy(alpha = 0.2f),
                strokeWidth = 5.dp
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "SOS",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                if (pressing) "..." else "3 seg",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

fun sendSosToContacts(
    context: Context,
    contacts: List<SosContact>,
    userName: String,
    lat: Double?,
    lng: Double?
) {
    val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    val locationText = if (lat != null && lng != null)
        "https://maps.google.com/?q=$lat,$lng" else "Ubicación no disponible"

    val message = "🚨 ALERTA SOS - RideUJAT\n" +
        "$userName necesita ayuda urgente.\n\n" +
        "📍 Ubicación: $locationText\n" +
        "🕐 Hora: $timeStr\n\n" +
        "Enviado desde RideUJAT"

    contacts.forEach { contact ->
        try {
            val phone = contact.telefono.filter { it.isDigit() }.let {
                if (!it.startsWith("52")) "52$it" else it
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) { /* WhatsApp no instalado */ }
    }
}
