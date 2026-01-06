package com.example.digiscan_onu_jc.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.leer_escribir_compose_googlesheets.ONU
import android.view.ViewGroup.LayoutParams

@Composable
fun ScannerScreen(
    innerPadding: PaddingValues,
    scanResult: String,
    listaONU: List<ONU>,
    estaSincronizando: Boolean,
    cargandoDatos: Boolean,
    // Pasamos la función desde la Activity
    onStartCamera: (PreviewView) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f),
            factory = { ctx ->
                // Creamos la vista aquí
                PreviewView(ctx).apply {
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                // Cuando la vista se actualiza/crea, iniciamos la cámara pasándole esta vista
                onStartCamera(view)
            }
        )

        // Estado de Sincronización
        if (!cargandoDatos) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(10.dp)
                        .background(
                            color = if (estaSincronizando) Color.Blue else Color(0xFF4CAF50),
                            shape = CircleShape
                        )
                )
                Text(
                    text = if (estaSincronizando) "Actualizando..." else "✅ Listo - Siguiente: ${calcularSiguienteNumero(listaONU)}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Resultados del Escaneo Actual
        Text(
            modifier = Modifier.padding(16.dp),
            text = scanResult,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
fun calcularSiguienteNumero(lista: List<ONU>): String {
    val NUMERO_INICIAL = 2000
    if (lista.isEmpty()) return NUMERO_INICIAL.toString()
    val ultimoNumero = lista.mapNotNull { it.numero?.toIntOrNull() }.maxOrNull()
    return if (ultimoNumero != null) (ultimoNumero + 1).toString() else NUMERO_INICIAL.toString()
}