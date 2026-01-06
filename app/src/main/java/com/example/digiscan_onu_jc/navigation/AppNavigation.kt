package com.example.digiscan_onu_jc.navigation

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.digiscan_onu_jc.screens.HistoryScreen
import com.example.digiscan_onu_jc.screens.ScannerScreen
import com.example.leer_escribir_compose_googlesheets.ONU

@Composable
fun AppNavigation(
    navController: NavHostController,
    innerPadding: PaddingValues,
    scanResult: String,
    listaONU: List<ONU>,
    estaSincronizando: Boolean,
    cargandoDatos: Boolean,
    onStartCamera: (PreviewView) -> Unit // <--- Nueva función
) {
    NavHost(navController, startDestination = AppScreens.ScannerScreen.route) {
        composable(AppScreens.ScannerScreen.route) {
            ScannerScreen(
                innerPadding,
                scanResult,
                listaONU,
                estaSincronizando,
                cargandoDatos,
                onStartCamera
            )
        }
        composable(AppScreens.HistoryScreen.route) {
            HistoryScreen(listaONU, innerPadding, estaSincronizando, {})
        }
    }
}