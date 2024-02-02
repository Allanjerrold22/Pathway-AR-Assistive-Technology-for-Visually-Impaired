package com.lowjunkie.esummit.models

import java.io.Serializable

data class Geometry(
    val coordinates: List<List<Double>>,
    val type: String
): Serializable