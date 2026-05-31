package mx.ujat.dacyti.rideujat.ui.vehicles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import mx.ujat.dacyti.rideujat.data.model.Vehicle

@Composable
fun VehicleFormDialog(
    vehicle: Vehicle? = null,
    onDismiss: () -> Unit,
    onConfirm: (marca: String, modelo: String, color: String, placas: String) -> Unit
) {
    val isEdit = vehicle != null

    var marca by remember { mutableStateOf(vehicle?.marca ?: "") }
    var modelo by remember { mutableStateOf(vehicle?.modelo ?: "") }
    var color by remember { mutableStateOf(vehicle?.color ?: "") }
    var placas by remember { mutableStateOf(vehicle?.placas ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Editar vehículo" else "Agregar vehículo") },
        text = {
            Column {
                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it; error = null },
                    label = { Text("Marca") },
                    placeholder = { Text("Honda, Toyota, Nissan…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = modelo,
                    onValueChange = { modelo = it; error = null },
                    label = { Text("Modelo") },
                    placeholder = { Text("Civic, Corolla, Versa…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it; error = null },
                    label = { Text("Color") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = placas,
                    onValueChange = { placas = it.uppercase(); error = null },
                    label = { Text("Placas") },
                    placeholder = { Text("ABC-1234") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    marca.isBlank() -> "La marca es requerida"
                    modelo.isBlank() -> "El modelo es requerido"
                    color.isBlank() -> "El color es requerido"
                    placas.isBlank() -> "Las placas son requeridas"
                    placas.length < 6 -> "Las placas deben tener al menos 6 caracteres"
                    else -> null
                }
                if (error == null) onConfirm(marca.trim(), modelo.trim(), color.trim(), placas.trim())
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
