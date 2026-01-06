package com.example.digiscan_onu_jc.navigation

sealed class AppScreens (val route: String) {
    object ScannerScreen : AppScreens("scanner_screen")
    object HistoryScreen : AppScreens("history_screen")
}

