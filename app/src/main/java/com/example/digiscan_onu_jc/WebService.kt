package com.example.leer_escribir_compose_googlesheets

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WebService {

    @GET("exec?spreadsheetId=19W83_Dolcg6pa_QAEu_-f93npP-02b_YzEvYooTQvK8&sheet=databaseonu")
    suspend fun obtenerTodoOnu()
    : Response<GetResponse>

    @POST("exec")
    suspend fun agregarONU(
        @Body ONU: ONUData
    ): Response<PostResponse>




}