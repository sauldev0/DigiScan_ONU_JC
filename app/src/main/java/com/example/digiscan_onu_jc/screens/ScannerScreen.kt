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
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.digiscan_onu_jc.utils.obtenerLogoFabricante
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 1. Modelo de datos para la notificación
data class OnuNotif(
    val id: Long = System.nanoTime(), // ID único basado en tiempo nanosegundo
    val onu: ONU,
    val esExito: Boolean = true
)

// 2. Componente de Tarjeta Individual (Gestiona su propio tiempo)
@Composable
fun NotificacionOnuItem(
    notif: OnuNotif,
    onDesaparecer: (OnuNotif) -> Unit
) {
    var esExitoLocal by remember { mutableStateOf(notif.esExito) }
    var visible by remember { mutableStateOf(true) }

    // El temporizador vive dentro de cada tarjeta
    LaunchedEffect(notif.id) {
        delay(2500) // 2.5 seg en verde
        esExitoLocal = false

        delay(7500) // 7.5 seg adicionales (10 total)
        visible = false // Inicia animación de salida

        delay(500) // Tiempo para que termine la animación
        onDesaparecer(notif)
    }

    val animatedColor by animateColorAsState(
        targetValue = if (esExitoLocal) Color(0xFF4CAF50) else Color.White,
        animationSpec = tween(1000)
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = animatedColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = obtenerLogoFabricante(notif.onu.serial)),
                    contentDescription = null,
                    modifier = Modifier.size(45.dp)
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "N° ${notif.onu.numero}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (esExitoLocal) Color.White else Color.Black
                    )
                    Text(
                        text = "MAC: ${notif.onu.mac}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (esExitoLocal) Color.White else Color.DarkGray
                    )
                    Text(
                        text = "SN: ${notif.onu.serial}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (esExitoLocal) Color.White else Color.DarkGray
                    )
                }
            }
        }
    }
}

// 3. Pantalla Principal del Escáner
@Composable
fun ScannerScreen(
    innerPadding: PaddingValues,
    scanResult: String,
    listaONU: List<ONU>,
    estaSincronizando: Boolean,
    cargandoDatos: Boolean,
    onStartCamera: (PreviewView) -> Unit,
    onFlashToggle: (Boolean) -> Unit
) {
    val colaCards = remember { mutableStateListOf<OnuNotif>() }

    // 1. Variable para ignorar el primer disparo del LaunchedEffect
    var inicializado by remember { mutableStateOf(false) }
    var flashEncendido by remember { mutableStateOf(false) } // Estado local del botón

    // Lógica para elegir la animación
    val lottieRes = when {
        scanResult.contains("REGISTRADO") || scanResult.contains("MEZCLADOS") -> R.raw.scanner_red
        scanResult.contains("¡Registro Exitoso!") ||
                (scanResult.contains("MAC OK") && scanResult.contains("PON OK")) -> R.raw.scanner_green
        else -> R.raw.scanner_default // La animación por defecto
    }

    // Lógica para el color del marco
    val colorMarco = when {
        scanResult.contains("REGISTRADO") || scanResult.contains("MEZCLADOS") -> MaterialTheme.colorScheme.error // Rojo si ya existe
        scanResult.contains("¡Registro Exitoso!") || (scanResult.contains("MAC OK") && scanResult.contains("PON OK")) -> Color(0xFF4CAF50) // Verde éxito
        else -> Color(0xFF6666FF) // Morado por defecto
    }

    LaunchedEffect(listaONU.size) {
        // 2. Si la lista tiene datos y es la primera vez, marcamos como inicializado y NO añadimos nada
        if (listaONU.isNotEmpty() && !inicializado) {
            inicializado = true
            return@LaunchedEffect
        }

        // 3. A partir de aquí, solo se ejecutará cuando la lista crezca tras el escaneo
        if (listaONU.isNotEmpty() && inicializado) {
            val nuevaOnu = listaONU.last()

            if (colaCards.none { it.onu.mac == nuevaOnu.mac && it.onu.numero == nuevaOnu.numero }) {
                colaCards.add(OnuNotif(onu = nuevaOnu))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().weight(0.6f)) {
            // 1. Cámara
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    }
                },
                update = { view -> onStartCamera(view) }
            )

            // 2. Indicador superior (Siguiente ONU)
            if (!cargandoDatos) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), CircleShape))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Siguiente: ${calcularSiguienteNumero(listaONU)}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            IconButton(
                onClick = {
                    flashEncendido = !flashEncendido
                    onFlashToggle(flashEncendido)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = if (flashEncendido) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Linterna",
                    tint = if (flashEncendido) Color.Yellow else Color.White
                )
            }

            // 3. Guía Visual
            DotLottieAnimation(
                source = DotLottieSource.Res(lottieRes),
                autoplay = true,
                loop = true,
                modifier = Modifier.size(280.dp).align(Alignment.Center)
            )

            Icon(
                painter = painterResource(id = R.drawable.esquinas_escaner),
                contentDescription = "Marco Escáner",
                modifier = Modifier.size(328.dp).align(Alignment.Center),
                tint = colorMarco// Ajusta este valor (0.1 a 1.0) para que las esquinas encajen perfecto
            )

            // 4. COLA DE CARDS (Apiladas abajo)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                colaCards.forEach { item ->
                    key(item.id) {
                        NotificacionOnuItem(
                            notif = item,
                            onDesaparecer = { colaCards.remove(it) }
                        )
                    }
                }
            }
        }

        // 5. Texto de estado del escáner (Separado por colores)
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dividimos el scanResult por el salto de línea que envías desde handleBarcode
            val lineas = scanResult.split("\n")

            lineas.forEach { linea ->
                Text(
                    text = linea,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    fontWeight = if (linea.contains("OK") || linea.contains("Exitoso")) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        // Caso: Éxito total o MAC/PON detectado individualmente
                        linea.contains("OK") || linea.contains("¡Registro Exitoso!") -> Color(0xFF4CAF50) // Verde

                        // Caso: Equipo ya registrado (Error)
                        linea.contains("REGISTRADO") -> MaterialTheme.colorScheme.error // Rojo

                        // Caso: Aún buscando o estado inicial
                        linea.contains("Buscando") -> Color.Gray

                        linea.contains("MEZCLADOS") -> MaterialTheme.colorScheme.error

                        else -> MaterialTheme.colorScheme.primary // Azul/Morado por defecto
                    }
                )
            }
        }
    }
}

fun calcularSiguienteNumero(lista: List<ONU>): String {
    val NUMERO_INICIAL = 2000
    if (lista.isEmpty()) return NUMERO_INICIAL.toString()
    val ultimoNumero = lista.mapNotNull { it.numero?.toIntOrNull() }.maxOrNull()
    return if (ultimoNumero != null) (ultimoNumero + 1).toString() else NUMERO_INICIAL.toString()
}