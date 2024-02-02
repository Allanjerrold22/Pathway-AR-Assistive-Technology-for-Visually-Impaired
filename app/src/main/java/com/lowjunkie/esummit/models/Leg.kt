package com.lowjunkie.esummit.models

data class Leg(
    val admins: List<Admin>,
    val distance: Double,
    val duration: Double,
    val steps: List<Step>,
    val summary: String,
    val via_waypoints: List<Any>,
    val weight: Double
)