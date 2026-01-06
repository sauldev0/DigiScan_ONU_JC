package com.example.digiscan_onu_jc.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.digiscan_onu_jc.R
import com.example.leer_escribir_compose_googlesheets.ONU

@Composable
fun HistoryScreen(listaONU: List<ONU>, innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Historial de Equipos",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
        ) {
            items(listaONU.reversed()) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reutilizamos la función obtenerLogoFabricante (debe ser accesible)
                        Image(
                            painter = painterResource(id = obtenerLogoFabricante(item.serial)),
                            contentDescription = null,
                            modifier = Modifier.size(45.dp).padding(end = 12.dp)
                        )
                        Column {
                            Text(text = "N°: ${item.numero}", fontWeight = FontWeight.Bold)
                            Text(text = "MAC: ${item.mac}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "SERIAL: ${item.serial}", style = MaterialTheme.typography.bodySmall)
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