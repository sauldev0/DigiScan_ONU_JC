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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class MainActivity : ComponentActivity() {

    private var previewView: PreviewView? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner

    private val MAC_REGEX = Regex("([0-9A-Fa-f]{2}[:.-]?){5}[0-9A-Fa-f]{2}")
    private val PON_SN_REGEX = Regex("^[A-Z]{4}[0-9A-F]{8}$")

    private var macDetectada: String? = null
    private var ponDetectada: String? = null
    private var estaBloqueado = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()

        setContent {
            DigiScan_ONU_JCTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BarcodeScannerScreen(innerPadding)

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    @Composable
    fun BarcodeScannerScreen(innerPadding: PaddingValues) {
        var scanResult by remember { mutableStateOf("Escanea un codigo") }
        val context = LocalContext.current

        val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted){
                startCamera(context, { result -> scanResult = result })
            } else {
                scanResult = "Permisos de camara necesarios"
            }
        }

        LaunchedEffect(true) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                factory = {context ->
                    PreviewView(context).apply{
                        previewView = this
                        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    }
                }

            )

            Text(
                modifier = Modifier
                    .padding(16.dp),
                text = scanResult,
                style = MaterialTheme.typography.bodyLarge

            )

        }

    }

    private fun startCamera(context: Context, onBarcodeDetected: (String) -> Unit){

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
                            processImageProxy(onBarcodeDetected, imageProxy)
                        })
                    }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            }

        }, ContextCompat.getMainExecutor(context) )

    }

    private fun processImageProxy(onBarcodeDetected: (String) -> Unit, imageProxy: ImageProxy){

        val mediaImage = imageProxy.image

        if (mediaImage != null){
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        handleBarcode(onBarcodeDetected, barcode, this@MainActivity)
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

    private fun handleBarcode(onUIUpdate: (String) -> Unit, barcode: Barcode, context: Context) {
        if (estaBloqueado) return
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
            finalizarCaptura(onUIUpdate, context)
        } else {
            // Actualizamos la UI con los "checks" parciales
            val statusMac = if (macDetectada != null) "✅ MAC OK" else "🔍 Buscando MAC..."
            val statusPon = if (ponDetectada != null) "✅ PON OK" else "🔍 Buscando PON SN..."
            onUIUpdate("$statusMac\n$statusPon")
        }
    }

    private fun finalizarCaptura(onUIUpdate: (String) -> Unit, context: Context) {
        if (estaBloqueado) return
        estaBloqueado = true
        vibrar(context)

        onUIUpdate("✅ EQUIPO COMPLETO\nMAC: $macDetectada\nPON: $ponDetectada")

        // Esperar 5 segundos y reiniciar
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            macDetectada = null
            ponDetectada = null
            estaBloqueado = false
            onUIUpdate("🔍 Escaneando siguiente equipo...")
        }, 5000)
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





}

