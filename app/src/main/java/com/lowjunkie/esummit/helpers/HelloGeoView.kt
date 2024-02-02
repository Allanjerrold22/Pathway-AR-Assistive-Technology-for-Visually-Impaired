/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lowjunkie.esummit.helpers

import android.opengl.GLSurfaceView
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.ar.core.Earth
import com.google.ar.core.GeospatialPose
import com.lowjunkie.esummit.HelloGeoActivity
import com.lowjunkie.esummit.R
import com.lowjunkie.esummit.java.common.helpers.SnackbarHelper
import kotlin.math.roundToInt

/** Contains UI elements for Hello Geo. */
class HelloGeoView(val activity: HelloGeoActivity) : DefaultLifecycleObserver {
  val root = View.inflate(activity, R.layout.hello_geo_screen, null)
  val surfaceView = root.findViewById<GLSurfaceView>(R.id.surfaceview)

  val session
    get() = activity.arCoreSessionHelper.session

  val snackbarHelper = SnackbarHelper()

  var mapView: MapView? = null

  val mapTouchWrapper = root.findViewById<MapTouchWrapper>(R.id.map_wrapper).apply {


      setup { screenLocation ->
          val latLng: LatLng =
              mapView?.googleMap?.projection?.fromScreenLocation(screenLocation) ?: return@setup
          activity.renderer.onMapClick(latLng)

      }
  }
  val mapFragment =
    (activity.supportFragmentManager.findFragmentById(R.id.map)!! as SupportMapFragment).also {
      it.getMapAsync { googleMap -> mapView = MapView(activity, googleMap) }
    }


    fun drawPoly(coordinates: List<List<Double>>){
        val polyline = mapView?.googleMap?.apply {
            addPolyline(

                PolylineOptions()
                    .clickable(true)
                    .add(
                        LatLng(coordinates!![0][1], coordinates!![0][1]),
                        LatLng(coordinates!![1][1], coordinates!![1][1]),
                        LatLng(-33.852, 151.211),

//            LatLng(-34.747, 145.592),
//            LatLng(-34.364, 147.891),
//            LatLng(-33.501, 150.217),
//            LatLng(-32.306, 149.248),
//            LatLng(-32.491, 147.309)
                    )
                    .width(5F)
                    .color(ContextCompat.getColor(activity.applicationContext, R.color.purple_500))
            )
        }
    }


    val statusText = root.findViewById<TextView>(R.id.statusText)
    val statusText2 = root.findViewById<TextView>(R.id.statusText2)

    fun updateStatusText(earth: Earth, cameraGeospatialPose: GeospatialPose?) {
    activity.runOnUiThread {
      val poseText = if (cameraGeospatialPose == null) "" else
        activity.getString(R.string.geospatial_pose,
                           cameraGeospatialPose.latitude,
                           cameraGeospatialPose.longitude,
                           cameraGeospatialPose.horizontalAccuracy,
                           cameraGeospatialPose.altitude,
                           cameraGeospatialPose.verticalAccuracy,
                           cameraGeospatialPose.heading,
                           cameraGeospatialPose.headingAccuracy)

        statusText.text = "Accuracy:" + String.format("%.3f", (cameraGeospatialPose?.horizontalAccuracy!! + cameraGeospatialPose?.verticalAccuracy!!)/2)

        statusText2.text = "Lat/Lon:" + String.format("%.3f", cameraGeospatialPose?.latitude) + "," + String.format("%.3f", cameraGeospatialPose?.longitude)
    }
  }

  override fun onResume(owner: LifecycleOwner) {
    surfaceView.onResume()
  }

  override fun onPause(owner: LifecycleOwner) {
    surfaceView.onPause()
  }
}
