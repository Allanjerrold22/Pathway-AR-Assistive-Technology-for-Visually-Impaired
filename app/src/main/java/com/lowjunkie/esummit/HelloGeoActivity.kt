package com.lowjunkie.esummit

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lowjunkie.esummit.models.Destination
import com.lowjunkie.esummit.models.Geometry

class HelloGeoActivity : AppCompatActivity() {
  companion object {
    private const val TAG = "HelloGeoActivity"
  }





  var destination : Destination?= null


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val extras = intent.extras
    var value : Geometry?= null
    if (extras != null) {
      destination = extras.getSerializable("key") as Destination

      //The key argument here must match that used in the other activity
    }





  }


}
