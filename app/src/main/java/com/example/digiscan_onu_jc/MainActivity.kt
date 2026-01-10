package com.example.digiscan_onu_jc

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SettingsOverscan
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.digiscan_onu_jc.ui.theme.DigiScan_ONU_JCTheme
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dotlottie.dlplayer.Mode
import com.example.digiscan_onu_jc.navigation.AppNavigation
import com.example.digiscan_onu_jc.navigation.AppScreens
import com.example.digiscan_onu_jc.screens.HistoryScreen
import com.example.digiscan_onu_jc.screens.ScannerScreen
import com.example.leer_escribir_compose_googlesheets.BaseUrl
import com.example.leer_escribir_compose_googlesheets.Constantes
import com.example.leer_escribir_compose_googlesheets.ONU
import com.example.leer_escribir_compose_googlesheets.ONUData
import com.example.leer_escribir_compose_googlesheets.RetrofitClient
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.lottiefiles.dotlottie.core.util.toColor
import com.lottiefiles.dotlottie.core.widget.DotLottieAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext




class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner

    private val MAC_REGEX = Regex("([0-9A-Fa-f]{2}[:.-]?){5}[0-9A-Fa-f]{2}")
    private val PON_SN_REGEX = Regex("^[A-Z]{4}[0-9A-F]{8}$")

    private var macDetectada: String? = null
    private var ponDetectada: String? = null
    private var estaBloqueado = false

    private val NUMERO_INICIAL = 2000

    private var camera: Camera? = null // Para controlar flash


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()

        setContent {
            DigiScan_ONU_JCTheme {

                // 1. Estados Globales
                var rutaActual by remember { mutableStateOf(AppScreens.ScannerScreen.route) }
                var mostrarCarga by remember { mutableStateOf(true) }
                var listaONU by remember { mutableStateOf(emptyList<ONU>()) }
                var scanResult by remember { mutableStateOf("Escanea un codigo") }
                var estaSincronizando by remember { mutableStateOf(false) }
                var cargandoDatos by remember { mutableStateOf(true) }

                // Permisos de camara
                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        scanResult = "Permisos de cámara necesarios"
                    }
                }

                // 2. LOGICA DE CARGA
                LaunchedEffect(Unit) {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)

                    // Solo una carga inicial al abrir la app
                    estaSincronizando = true
                    obtenerData {
                        listaONU = it
                        cargandoDatos = false
                        estaSincronizando = false
                        mostrarCarga = false
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        bottomBar = {
                            if (!mostrarCarga) {
                                // Barra de navegación personalizada (ya no requiere navController)
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = rutaActual == AppScreens.ScannerScreen.route,
                                        onClick = { rutaActual = AppScreens.ScannerScreen.route },
                                        icon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.barcode_scan_icon),
                                                contentDescription = "Escáner",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = { Text("Escáner") }
                                    )
                                    NavigationBarItem(
                                        selected = rutaActual == AppScreens.HistoryScreen.route,
                                        onClick = {
                                            rutaActual = AppScreens.HistoryScreen.route
                                            // Disparamos una actualización manual al entrar al historial
                                            estaSincronizando = true
                                            obtenerData {
                                                listaONU = it
                                                estaSincronizando = false
                                            }
                                        },
                                        icon = { Icon(Icons.Default.History, null) },
                                        label = { Text("Historial") }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        // Pilas de pantallas en lugar de navegación destructiva
                        Box(modifier = Modifier.fillMaxSize()) {

                            // Capa 1 (Fondo): El Escáner siempre está activo
                            ScannerScreen(
                                innerPadding = innerPadding,
                                scanResult = scanResult,
                                listaONU = listaONU,
                                estaSincronizando = estaSincronizando,
                                cargandoDatos = cargandoDatos,
                                onStartCamera = { vistaRecibida ->
                                    // Ya no necesitamos validar la ruta, la cámara inicia una sola vez
                                    startCamera(
                                        context = this@MainActivity,
                                        previewView = vistaRecibida,
                                        onBarcodeDetected = { scanResult = it },
                                        onRefreshList = { listaONU = it },
                                        obtenerListaActual = { listaONU },
                                        obtenerCargando = { cargandoDatos },
                                        estaEnPantallaEscaner = { rutaActual == AppScreens.ScannerScreen.route }
                                    )
                                },
                                onFlashToggle = { encendido ->
                                    toggleFlash(encendido) // Llama a la lógica de la cámara
                                }
                            )

                            // Capa 2 (Frente): El Historial se dibuja ENCIMA solo si se selecciona
                            if (rutaActual == AppScreens.HistoryScreen.route) {
                                HistoryScreen(
                                    listaONU = listaONU,
                                    innerPadding = innerPadding,
                                    estaSincronizando = estaSincronizando,
                                    onRefresh = {
                                        estaSincronizando = true
                                        obtenerData {
                                            listaONU = it
                                            estaSincronizando = false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Capa 3 (Superior absoluta): Animación de carga
                    if (mostrarCarga) PantallaDeCarga()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    private fun startCamera(
        context: Context,
        previewView: PreviewView, // <--- Ahora lo recibimos directamente
        onBarcodeDetected: (String) -> Unit,
        onRefreshList: (List<ONU>) -> Unit,
        obtenerListaActual: () -> List<ONU>,
        obtenerCargando: () -> Boolean,
        estaEnPantallaEscaner: () -> Boolean
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build()

            // Asignamos el surface provider directamente al objeto recibido
            preview.surfaceProvider = previewView.surfaceProvider

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, { imageProxy ->
                        processImageProxy(onBarcodeDetected, imageProxy, onRefreshList, obtenerListaActual(), obtenerCargando(), estaEnPantallaEscaner())
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e("CameraX", "Error al vincular cámara", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // FUNCIÓN PARA EL TOGGLE DEL FLASH
    private fun toggleFlash(encendido: Boolean) {
        camera?.cameraControl?.enableTorch(encendido)
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(onBarcodeDetected: (String) -> Unit, imageProxy: ImageProxy, onRefreshList: (List<ONU>) -> Unit, listaActual: List<ONU>, cargandoDatos: Boolean, escaneoActivo: Boolean){

        val mediaImage = imageProxy.image

        // Salida rápida: Si no hay imagen o el escáner debe estar apagado
        if (mediaImage == null || !escaneoActivo) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    handleBarcode(onBarcodeDetected, barcode, this@MainActivity, onRefreshList, listaActual, cargandoDatos)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se detecto codigo", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun handleBarcode(onUIUpdate: (String) -> Unit, barcode: Barcode, context: Context, onRefreshList: (List<ONU>) -> Unit, listaActual: List<ONU>, cargandoDatos: Boolean) {
        if (estaBloqueado || (listaActual.isEmpty() && cargandoDatos)) return
        val valor = barcode.rawValue ?: return

        when {
            // Filtro MAC
            valor.matches(MAC_REGEX) -> {
                val tieneLetras = valor.any { it.isLetter() }
                val tieneSeparadores = valor.contains(":") || valor.contains("-") || valor.contains(".")
                if (tieneLetras || tieneSeparadores) {
                    // VALIDACIÓN: ¿Esta MAC ya existe?
                    val yaExiste = listaActual.any { it.mac?.equals(valor, ignoreCase = true) == true }

                    if (yaExiste) {
                        vibrar(context)
                        macDetectada = null
                        ponDetectada = null
                        onUIUpdate("EQUIPO YA REGISTRADO")

                        estaBloqueado = true
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            estaBloqueado = false
                            onUIUpdate("Escaneando siguiente equipo...")
                        }, 3000)
                        return
                    }
                    macDetectada = valor
                }
            }
            // Filtro PON SN
            valor.matches(PON_SN_REGEX) -> {
                // VALIDACIÓN: ¿Este Serial ya existe?
                val yaExisteSerial = listaActual.any { it.serial?.equals(valor, ignoreCase = true) == true }

                if (yaExisteSerial) {
                    vibrar(context)
                    macDetectada = null
                    ponDetectada = null
                    onUIUpdate("EQUIPO YA REGISTRADO")

                    estaBloqueado = true
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        estaBloqueado = false
                        onUIUpdate("Escaneando siguiente equipo...")
                    }, 3000)
                    return
                }
                ponDetectada = valor
            }
        }

        // Si ya tenemos ambos, disparamos el éxito
        if (macDetectada != null && ponDetectada != null) {
            finalizarCaptura(onUIUpdate, context, onRefreshList, listaActual)
        } else {
            // Actualizamos la UI con los "checks" parciales
            val statusMac = if (macDetectada != null) "MAC OK" else "Buscando MAC..."
            val statusPon = if (ponDetectada != null) "PON OK" else "Buscando PON SN..."
            onUIUpdate("$statusMac\n$statusPon")
        }
    }

    private fun finalizarCaptura(onUIUpdate: (String) -> Unit, context: Context, onRefreshList: (List<ONU>) -> Unit, listaActual: List<ONU>) {
        if (estaBloqueado) return
        estaBloqueado = true
        vibrar(context)

        // Capturamos valores locales
        val macParaEnviar = macDetectada ?: ""
        val ponParaEnviar = ponDetectada ?: ""

        onUIUpdate("Enviando a Google Sheets...")

        // OPTIMIZACIÓN: Enviamos el número vacío (""), el Script lo pondrá.
        agregarData(
            numero = "",
            mac = macParaEnviar,
            serial = ponParaEnviar
        ) {
            // Al terminar el envío, refrescamos el historial para ver el resultado real
            obtenerData { newList ->
                onRefreshList(newList)
                onUIUpdate("¡Registro Exitoso!")

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    macDetectada = null
                    ponDetectada = null
                    estaBloqueado = false
                    onUIUpdate("Escanea un codigo")
                }, 2000)
            }
        }
    }


    private fun vibrar(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    private fun agregarData(numero: String, mac: String, serial: String, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            // Estructura de columnas según Google Sheet
            val row = listOf("", numero, "", mac, "", serial, "")

            val onuData = ONUData(
                spreadsheet_id = Constantes.google_sheet_id,
                sheet = Constantes.sheet,
                rows = listOf(row)
            )

            val response = RetrofitClient.webService(BaseUrl.base_url_post).agregarONU(onuData)

            if (response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    private fun obtenerData(onDataReceived: (List<ONU>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.webService(BaseUrl.base_url_get).obtenerTodoOnu()

                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        onDataReceived(response.body()?.databaseonu ?: emptyList())
                    }
                }
            } catch (e: Exception) {
                // En caso de error de red, podrías mostrar un Toast
            }
        }
    }

    private fun calcularSiguienteNumero(lista: List<ONU>): String { 
        if (lista.isEmpty()) return NUMERO_INICIAL.toString()

        // convertir el campo 'numero' a entero para encontrar el máximo
        val ultimoNumero = lista.mapNotNull { it.numero?.toIntOrNull() }.maxOrNull()

        return if (ultimoNumero != null) {
            (ultimoNumero + 1).toString()
        } else {
            NUMERO_INICIAL.toString()
        }
    }

}

@Composable
fun PantallaDeCarga() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(0.dp) // Margen general para que nada toque los bordes
    ) {
        // 1. LOGO (Siempre arriba)
        Column(
            modifier = Modifier
                .padding(top = 0.dp) // Espacio desde la cámara/borde superior
                .align(Alignment.TopCenter),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_app_digiscan),
                contentDescription = "Logo DigiScan",
                modifier = Modifier.size(350.dp)
            )
        }

        // 2. ANIMACIÓN + TEXTO (CENTRO ABSOLUTO)
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DotLottieAnimation(
                source = DotLottieSource.Res(R.raw.loading_animation),
                autoplay = true,
                loop = true,
                speed = 3f,
                modifier = Modifier.size(300.dp)
            )

            Text(
                text = "Sincronizando con Google Sheets...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 3. CRÉDITOS (Siempre abajo)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Developed by",
                style = MaterialTheme.typography.labelLarge,
                color = Color.LightGray.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )
            Text(
                text = "sauldev",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray,
                letterSpacing = 2.sp
            )
        }
    }
}








