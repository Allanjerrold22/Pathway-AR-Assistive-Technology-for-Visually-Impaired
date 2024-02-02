package com.lowjunkie.esummit

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.lowjunkie.esummit.helpers.ARCoreSessionLifecycleHelper
import com.lowjunkie.esummit.helpers.HelloGeoView
import com.google.ar.core.exceptions.*
import com.lj.smartnavigator.helpers.GeoPermissionsHelper
import com.lowjunkie.esummit.api.RetrofitInstance
import com.lowjunkie.esummit.java.common.helpers.FullScreenHelper
import com.lowjunkie.esummit.java.common.samplerender.SampleRender
import com.lowjunkie.esummit.models.Destination
import com.lowjunkie.esummit.models.Geometry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HelloGeoActivity : AppCompatActivity() {
  companion object {
    private const val TAG = "HelloGeoActivity"
  }

  lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper
  lateinit var view: HelloGeoView
  lateinit var renderer: HelloGeoRenderer





  var destination : Destination ?= null


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val extras = intent.extras
    var value : Geometry?= null
    if (extras != null) {
      destination = extras.getSerializable("key") as Destination

      //The key argument here must match that used in the other activity
    }





    // Setup ARCore session lifecycle helper and configuration.
    arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)

    arCoreSessionHelper.exceptionCallback =
      { exception ->
        val message =
          when (exception) {
            is UnavailableUserDeclinedInstallationException ->
              "Please install Google Play Services for AR"
            is UnavailableApkTooOldException -> "Please update ARCore"
            is UnavailableSdkTooOldException -> "Please update this app"
            is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
            is CameraNotAvailableException -> "Camera not available. Try restarting the app."
            else -> "Failed to create AR session: $exception"
          }
        Log.e(TAG, "ARCore threw an exception", exception)
        view.snackbarHelper.showError(this, message)
      }

    // Configure session features.
    arCoreSessionHelper.beforeSessionResume = ::configureSession
    lifecycle.addObserver(arCoreSessionHelper)

    // Set up the Hello AR renderer.
    view = HelloGeoView(this)
    lifecycle.addObserver(view)
    renderer = HelloGeoRenderer(this,
      destination!!
      //  value
    )
    lifecycle.addObserver(renderer)

    // Set up Hello AR UI.

    setContentView(view.root)

    // Sets up an example renderer using our HelloGeoRenderer.
    SampleRender(view.surfaceView, renderer, assets)
  }

  // Configure the session, setting the desired options according to your usecase.
  fun configureSession(session: Session) {
    // TODO: Configure ARCore to use GeospatialMode.ENABLED.
    session.configure(
      session.config.apply {
        geospatialMode = Config.GeospatialMode.ENABLED
      }
    )
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    results: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, results)
    if (!GeoPermissionsHelper.hasGeoPermissions(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "Camera and location permissions are needed to run this application", Toast.LENGTH_LONG)
        .show()
      if (!GeoPermissionsHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        GeoPermissionsHelper.launchPermissionSettings(this)
      }
      finish()
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
  }
}
