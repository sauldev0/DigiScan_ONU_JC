package com.example.leer_escribir_compose_googlesheets

data class PostResponse(
    val rows: List<List<String>>,
    val sheet: String

)
