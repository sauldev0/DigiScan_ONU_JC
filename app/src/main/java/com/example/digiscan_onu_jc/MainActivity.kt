package com.example.digiscan_onu_jc

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.dotlottie.dlplayer.Mode
import com.example.leer_escribir_compose_googlesheets.BaseUrl
import com.example.leer_escribir_compose_googlesheets.Constantes
import com.example.leer_escribir_compose_googlesheets.ONU
import com.example.leer_escribir_compose_googlesheets.ONUData
import com.example.leer_escribir_compose_googlesheets.RetrofitClient
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.lottiefiles.dotlottie.core.widget.DotLottieAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



class MainActivity : ComponentActivity() {

    private var previewView: PreviewView? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner

    private val MAC_REGEX = Regex("([0-9A-Fa-f]{2}[:.-]?){5}[0-9A-Fa-f]{2}")
    private val PON_SN_REGEX = Regex("^[A-Z]{4}[0-9A-F]{8}$")

    private var macDetectada: String? = null
    private var ponDetectada: String? = null
    private var estaBloqueado = false

    private val NUMERO_INICIAL = 2000


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()

        setContent {
            DigiScan_ONU_JCTheme {
                var mostrarCarga by remember { mutableStateOf(true) }
                Box(modifier = Modifier.fillMaxSize()) {
                    // Pantalla principal de la App
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        BarcodeScannerScreen(
                            innerPadding = innerPadding,
                            onDatosListos = {
                                // Cuando obtenerData responda, oculta el Lottie
                                mostrarCarga = false
                            }
                        )
                    }

                    // Capa superior: Pantalla de Carga
                    if (mostrarCarga) {
                        PantallaDeCarga()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    @Composable
    fun BarcodeScannerScreen(innerPadding: PaddingValues, onDatosListos: () -> Unit) {
        var scanResult by remember { mutableStateOf("Escanea un codigo") }
        var listaONU by remember { mutableStateOf(emptyList<ONU>()) }

        var cargandoDatos by remember { mutableStateOf(true) }
        var estaSincronizando by remember { mutableStateOf(false) } // Para el efecto visual

        val context = LocalContext.current

        val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted){
                startCamera(context, { result -> scanResult = result },{ newList -> listaONU = newList }, {listaONU}, { cargandoDatos })
            } else {
                scanResult = "Permisos de camara necesarios"
            }
        }

        LaunchedEffect(true) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)

            // CARGA INICIAL INMEDIATA
            estaSincronizando = true
            obtenerData { newList ->
                listaONU = newList
                cargandoDatos = false // Desbloquea la interfaz rápido
                estaSincronizando = false
                onDatosListos()
            }

            // BUCLE DE ACTUALIZACIÓN (Polling)
            while (true){
                delay(30000) // Espera 30 segundos antes de volver a consultar
                estaSincronizando = true // Inicia parpadeo
                obtenerData { newList ->
                    listaONU = newList
                    cargandoDatos = false // Ya tenemos los datos, liberamos el escaneo
                    estaSincronizando = false
            }

            }
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f),
                factory = {context ->
                    PreviewView(context).apply{
                        previewView = this
                        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    }
                }

            )

            if (cargandoDatos) {
                Text(
                    text = "⏳ Sincronizando con Google Sheets...",
                    color = Color.Red,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Círculo pequeño
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(10.dp)
                            .background(
                                color = if (estaSincronizando) Color.Blue else Color(0xFF4CAF50),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )

                    Text(
                        text = if (estaSincronizando) "Actualizando datos..." else "✅ Sistema Listo - Siguiente N°: ${calcularSiguienteNumero(listaONU)}",
                        color = if (estaSincronizando) Color.Blue else Color(0xFF4CAF50),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Text(
                modifier = Modifier
                    .padding(16.dp),
                text = scanResult,
                style = MaterialTheme.typography.bodyLarge

            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .padding(8.dp)
            ) {
                items(listaONU.reversed()){item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // MOSTRAR EL PNG
                            Image(
                                painter = painterResource(id = obtenerLogoFabricante(item.serial)),
                                contentDescription = "Fabricante",
                                modifier = Modifier
                                    .size(45.dp) // Tamaño en pantalla
                                    .padding(end = 12.dp)
                            )

                        Column(
                            modifier = Modifier
                                .padding(8.dp)

                        ) {
                            Text(text = "N°: ${item.numero}")
                            Text(text = "MAC: ${item.mac}")
                            Text(text = "SERIAL: ${item.serial}")
                        }
                    }
                }

            }

        }

        }


    }

    @Composable
    fun obtenerLogoFabricante(serial: String?): Int {
        // Tomamos las primeras 4 letras del serial (PON SN)
        val prefijo = serial?.take(4)?.uppercase() ?: ""

        return when (prefijo) {
            "VSOL" -> R.drawable.vsol_logo  // Nombre de tu archivo png en drawable
            // "HWTC", "HUAW" -> R.drawable.huawei_logo
            // "ZTEG" -> R.drawable.zte_logo
            //"FHTT" -> R.drawable.fiberhome_logo
            // "TPLN" -> R.drawable.tplink_logo
            else -> R.drawable.generic_onu_logo // Un logo por defecto
        }
    }

    private fun startCamera(context: Context, onBarcodeDetected: (String) -> Unit, onRefreshList: (List<ONU>) -> Unit, obtenerListaActual: () -> List<ONU>, obtenerCargando: () -> Boolean){

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build()

            previewView?.let { previewView ->
                preview.surfaceProvider = previewView.surfaceProvider

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, { imageProxy ->
                            processImageProxy(onBarcodeDetected, imageProxy, onRefreshList, obtenerListaActual(), obtenerCargando())
                        })
                    }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            }

        }, ContextCompat.getMainExecutor(context) )

    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(onBarcodeDetected: (String) -> Unit, imageProxy: ImageProxy, onRefreshList: (List<ONU>) -> Unit, listaActual: List<ONU>, cargandoDatos: Boolean){

        val mediaImage = imageProxy.image

        if (mediaImage != null){
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
    }

    private fun handleBarcode(onUIUpdate: (String) -> Unit, barcode: Barcode, context: Context, onRefreshList: (List<ONU>) -> Unit, listaActual: List<ONU>, cargandoDatos: Boolean) {
        if (estaBloqueado || listaActual.isEmpty() && cargandoDatos) return
        val valor = barcode.rawValue ?: return

        when {
            // Filtro MAC
            valor.matches(MAC_REGEX) -> {
                val tieneLetras = valor.any { it.isLetter() }
                val tieneSeparadores = valor.contains(":") || valor.contains("-") || valor.contains(".")
                if (tieneLetras || tieneSeparadores) macDetectada = valor
            }
            // Filtro PON SN
            valor.matches(PON_SN_REGEX) -> {
                ponDetectada = valor
            }
        }

        // Si ya tenemos ambos, disparamos el éxito
        if (macDetectada != null && ponDetectada != null) {
            finalizarCaptura(onUIUpdate, context, onRefreshList, listaActual)
        } else {
            // Actualizamos la UI con los "checks" parciales
            val statusMac = if (macDetectada != null) "✅ MAC OK" else "🔍 Buscando MAC..."
            val statusPon = if (ponDetectada != null) "✅ PON OK" else "🔍 Buscando PON SN..."
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

        //onUIUpdate("⏳ Verificando último número en la nube...")

        obtenerData { listaMasReciente ->
            // Actualizamos la lista local con lo que acabamos de bajar
            onRefreshList(listaMasReciente)

            val proximoNumero = calcularSiguienteNumero(listaMasReciente)

            onUIUpdate("✅ Capturado equipo N°$proximoNumero\nEnviando a Google Sheets...")

            // Lógica de envío
            agregarData(
                numero = proximoNumero,
                mac = macParaEnviar,
                serial = ponParaEnviar
            ) {
                obtenerData { newList ->
                    onRefreshList(newList)
                    onUIUpdate("✅ ENVIADO EXITOSAMENTE\nN°: $proximoNumero\nMAC: $macParaEnviar\nPON: $ponParaEnviar")

                    // Reiniciamos el escáner después de 5 segundos
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        macDetectada = null
                        ponDetectada = null
                        estaBloqueado = false
                        onUIUpdate("🔍 Escaneando siguiente equipo...")
                    }, 5000)
                }
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



    // preview simulacion
    @Preview()
    @Composable
    fun previewSystem(){
        DigiScan_ONU_JCTheme {
            var mostrarCarga by remember { mutableStateOf(true) }
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BarcodeScannerScreen(
                        innerPadding = innerPadding,
                        onDatosListos = {
                            mostrarCarga = false
                        }
                    )
                }
                if (mostrarCarga) {
                    PantallaDeCarga()
                }
            }
        }
    }
}

@Composable
fun PantallaDeCarga() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_app),
            contentDescription = "Logo DigiScan",
            modifier = Modifier.size(120.dp)
        )

        DotLottieAnimation(
            source = DotLottieSource.Res(R.raw.loading_animation),
            autoplay = true,
            loop = true,
            speed = 3f,
            useFrameInterpolation = false,
            playMode = Mode.FORWARD,
            modifier = Modifier.size(300.dp)
        )

        Text(
            text = "Sincronizando con Google Sheets...",
            style = MaterialTheme.typography.titleMedium,
            color = Color.DarkGray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}








