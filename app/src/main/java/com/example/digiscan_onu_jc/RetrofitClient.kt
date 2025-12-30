package com.example.leer_escribir_compose_googlesheets

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object RetrofitClient {

    fun webService(baseUrl: String): WebService {
        val webService: WebService by lazy {
            Retrofit
                .Builder()
                .baseUrl(baseUrl)
                .addConverterFactory((GsonConverterFactory.create(GsonBuilder().create())))
                .build()
                .create(WebService::class.java)
        }
        return webService
    }
}