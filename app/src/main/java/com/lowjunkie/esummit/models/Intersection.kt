package com.google.ar.core.codelabs.hellogeospatial.api

import com.lowjunkie.esummit.models.MapboxStreetsV8

data class Intersection(
    val admin_index: Int,
    val bearings: List<Int>,
    val duration: Double,
    val entry: List<Boolean>,
    val geometry_index: Int,
    val `in`: Int,
    val is_urban: Boolean,
    val location: List<Double>,
    val mapbox_streets_v8: MapboxStreetsV8,
    val `out`: Int,
    val weight: Double
)