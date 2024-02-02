package com.lowjunkie.esummit.api


import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitInstance {
    companion object{
        private val retrofit by lazy {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
            val gson = GsonBuilder()
                .setLenient()
                .create()
            Retrofit.Builder()
                .baseUrl("https://api.mapbox.com/directions/v5/mapbox/")
                .addConverterFactory(
                    GsonConverterFactory.create(gson))
                .client(client)
                .build()
        }
        val api by lazy {
            retrofit.create(DirectionsAPI::class.java)
        }
    }
}