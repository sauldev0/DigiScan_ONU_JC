package com.example.digiscan_onu_jc.utils

import androidx.compose.runtime.Composable
import com.example.digiscan_onu_jc.R

@Composable
fun obtenerLogoFabricante(serial: String?): Int {
    // Tomamos las primeras 4 letras del serial (PON SN)
    val prefijo = serial?.take(4)?.uppercase() ?: ""

    return when (prefijo) {
        "VSOL" -> R.drawable.vsol_logo  // Nombre archivo png en drawable
        "ADCT" -> R.drawable.adc_logo
        "HWTC", "HUAW" -> R.drawable.huawei_logo
        "ZTEG" -> R.drawable.zte_logo
        "TPLN" -> R.drawable.tplink_logo
        //"FHTT" -> R.drawable.fiberhome_logo
        else -> R.drawable.generic_onu_logo // Un logo por defecto
    }
}