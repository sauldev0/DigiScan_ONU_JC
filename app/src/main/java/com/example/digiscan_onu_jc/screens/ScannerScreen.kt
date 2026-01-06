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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.dotlottie.dlplayer.Mode
import com.example.digiscan_onu_jc.R

@Composable
fun ScannerScreen(
    innerPadding: PaddingValues,
    scanResult: String,
    listaONU: List<ONU>,
    estaSincronizando: Boolean,
    cargandoDatos: Boolean,
    onStartCamera: (PreviewView) -> Unit
) {
    val ultimaONU = listaONU.lastOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // CONTENEDOR DE CÁMARA + GUÍA + INDICADORES
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
        ) {
            // 1. La Cámara (Fondo)
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    }
                },
                update = { view -> onStartCamera(view) }
            )

            // Indicador puntito verde
            if (!cargandoDatos) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color = Color(0xFF4CAF50), shape = CircleShape)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Siguiente ONU: ${calcularSiguienteNumero(listaONU)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // 3. GUÍA VISUAL (Centrada)
            DotLottieAnimation(
                source = DotLottieSource.Res(R.raw.scanner),
                autoplay = true,
                loop = true,
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center) // <--- Centrado absoluto
            )

            // 4. CARD DEL EQUIPO RECIÉN REGISTRADO
            if (ultimaONU != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = obtenerLogoFabricante(ultimaONU.serial)),
                            contentDescription = null,
                            modifier = Modifier.size(50.dp)
                        )
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(
                                text = "Último Registro: N° ${ultimaONU.numero}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "MAC: ${ultimaONU.mac}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "SN: ${ultimaONU.serial}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // 5. RESULTADOS DEL ESCANEO ACTUAL (MAC OK / PON OK)
        Text(
            modifier = Modifier.padding(16.dp),
            text = scanResult,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )




    }
}
fun calcularSiguienteNumero(lista: List<ONU>): String {
    val NUMERO_INICIAL = 2000
    if (lista.isEmpty()) return NUMERO_INICIAL.toString()
    val ultimoNumero = lista.mapNotNull { it.numero?.toIntOrNull() }.maxOrNull()
    return if (ultimoNumero != null) (ultimoNumero + 1).toString() else NUMERO_INICIAL.toString()
}