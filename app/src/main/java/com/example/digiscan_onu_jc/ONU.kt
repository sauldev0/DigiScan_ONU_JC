package com.example.leer_escribir_compose_googlesheets

import com.google.gson.annotations.SerializedName

data class ONU(
    @SerializedName("N°")
    val numero: String,
    @SerializedName("MAC DE EQUIPO")
    val mac: String,
    @SerializedName("SERIAL DE EQUIPO")
    val serial: String
)

data class ONUData(
    val spreadsheet_id: String,
    val sheet: String,
    val rows: List<List<String>>
)
