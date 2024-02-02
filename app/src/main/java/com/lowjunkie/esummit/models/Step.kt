package com.lowjunkie.esummit.models

import com.google.ar.core.codelabs.hellogeospatial.api.Intersection

data class Step(
    val distance: Double,
    val driving_side: String,
    val duration: Double,
    val geometry: Geometry,
    val intersections: List<Intersection>,
    val maneuver: Maneuver,
    val mode: String,
    val name: String,
    val weight: Double
)