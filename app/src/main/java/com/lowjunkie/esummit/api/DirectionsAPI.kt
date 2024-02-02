package com.lowjunkie.esummit.api

import com.lowjunkie.esummit.models.Direction
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DirectionsAPI {
    @GET("walking/{coord}")
    suspend fun getPolyline(
        @Path("coord") coord: String,
        @Query("alternatives")
        alternatives: String = "true",
        @Query("continue_straight")
        continue_straight: String = "true",
        @Query("geometries")
        geometries: String = "geojson",
        @Query("language")
        language: String = "en",
        @Query("overview")
        overview: String = "simplified",
        @Query("steps")
        steps: String = "true",
        @Query("access_token")
        access_token : String = "pk.eyJ1IjoibG93anVua2llIiwiYSI6ImNsZWQ0NjVwcDAzY2czeHQ1YWd6OWkxc2sifQ.8_xyQ-o1-G0sywbw5hw_aQ"
    ): Response<Direction>


}