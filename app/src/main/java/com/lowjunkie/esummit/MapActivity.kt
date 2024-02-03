package com.lowjunkie.esummit

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import kotlinx.android.synthetic.main.map_screen.*
import kotlinx.android.synthetic.main.map_screen.view.*


class MapActivity: AppCompatActivity() {

    val POINT: Point = Point.fromLngLat( 80.046087,12.823530)
    val POINT2: Point = Point.fromLngLat( 80.047087,12.824530)

    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var pointAnnotation: PointAnnotation
    private val BOUNDS_ID = "BOUNDS_ID"
    private val MAP_BOUNDS: CameraBoundsOptions = CameraBoundsOptions.Builder()
        .bounds(
            CoordinateBounds(
                Point.fromLngLat(80.035411, 12.814117),
                Point.fromLngLat(80.054792, 12.837574),
                false
            )
        )
        .minZoom(12.0)
        .build()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.map_screen)
        val iconBitmap = convertDrawableToBitmap(AppCompatResources.getDrawable(this.applicationContext, R.drawable.red_marker))

        mapView.setOnClickListener {

        }
        mapView?.getMapboxMap()?.loadStyle(
            style(Style.SATELLITE_STREETS) {
                +geoJsonSource(BOUNDS_ID) {
                    featureCollection(FeatureCollection.fromFeatures(listOf()))
                }
            }
        ) {
            prepareAnnotationMarker(mapView!!, iconBitmap!!)
            setupBounds(MAP_BOUNDS)
        }

    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }


    private fun showBoundsArea(boundsOptions: CameraBoundsOptions) {
        val source = mapView?.getMapboxMap()?.getStyle()!!.getSource(BOUNDS_ID) as GeoJsonSource
        val bounds = boundsOptions.bounds
        val list = mutableListOf<List<Point>>()
        bounds?.let {
            val northEast = it.northeast
            val southWest = it.southwest
            val northWest = Point.fromLngLat(southWest.longitude(), northEast.latitude())
            val southEast = Point.fromLngLat(northEast.longitude(), southWest.latitude())
            list.add(
                mutableListOf(northEast, southEast, southWest, northWest, northEast)
            )
        }

        source.geometry(
            Polygon.fromLngLats(
                list
            )
        )
    }

    private fun setupBounds(bounds: CameraBoundsOptions) {
        mapView?.getMapboxMap()?.setBounds(bounds)
        showBoundsArea(bounds)
    }

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun prepareAnnotationMarker(mapView: MapView, iconBitmap: Bitmap) {
        val annotationPlugin = mapView.annotations
        val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
            .withPoint(POINT)
            .withIconImage(iconBitmap)
            .withIconAnchor(IconAnchor.BOTTOM)
            .withDraggable(false)


        val pointAnnotationOptions2: PointAnnotationOptions = PointAnnotationOptions()
            .withPoint(POINT2)
            .withIconImage(iconBitmap)
            .withIconAnchor(IconAnchor.BOTTOM)
            .withDraggable(false)

        pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
//        pointAnnotationManager.addClickListener(OnPointAnnotationClickListener {
//            val intent = Intent(this, Panoroma::class.java)
//            startActivity(intent)
//            true
//        })
        pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)
        pointAnnotationManager.create(pointAnnotationOptions2)
    }





}