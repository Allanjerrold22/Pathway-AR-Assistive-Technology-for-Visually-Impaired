package com.lowjunkie.esummit.models

data class Direction(
    val code: String,
    val routes: List<Route>,
    val uuid: String,
    val waypoints: List<Waypoint>
)