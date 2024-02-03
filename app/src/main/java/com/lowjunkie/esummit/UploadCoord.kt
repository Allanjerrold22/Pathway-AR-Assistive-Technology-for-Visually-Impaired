package com.lowjunkie.esummit

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.ar.core.Anchor
import com.google.firebase.firestore.FirebaseFirestore
import com.lowjunkie.esummit.models.Destination
import com.lowjunkie.esummit.models.Geometry
import kotlinx.android.synthetic.main.card_item.view.*
import kotlinx.android.synthetic.main.details_screen.*
import kotlinx.android.synthetic.main.upload_coord_screen.btnStop
import kotlinx.android.synthetic.main.upload_coord_screen.buttonRecord
import kotlinx.android.synthetic.main.upload_coord_screen.buttonSubmit
import kotlinx.android.synthetic.main.upload_coord_screen.coordName
import kotlinx.android.synthetic.main.upload_coord_screen.coordNameET
import kotlinx.android.synthetic.main.upload_coord_screen.coordTV

class UploadCoord: AppCompatActivity() {

    private var fireStore : FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val handler = Handler()
    private var isRecording = false

    private val recordRunnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            if (isRecording) {
                // Call your function here

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->

                        // Got last known location
                        if (location != null) {
                            val latitude = location.latitude
                            val longitude = location.longitude
                            listCoord.add(listOf(latitude.toString(), longitude.toString()))
                            Log.d("Rajesh",latitude.toString())
                            coordTV.setText(listCoord.toString())
                            // Use the latitude and longitude as needed
                            // Do something with the coordinates
                        }
                    }
                    .addOnFailureListener { e ->
                        // Handle failure
                        Log.d("Mohan","Fail")
                        e.printStackTrace()
                    }

                // Schedule the next execution after 5 seconds
                handler.postDelayed(this, 5000)
            }
        }
    }





    var listCoord = mutableListOf<List<String>>()

    fun postData(){



        fireStore.collection("destination").document(coordNameET.text.toString()).set(
            mutableMapOf(
            "type" to "indoor",
                "name" to coordNameET.text.toString(),
            "coord" to listCoord.toString(),
        )

        )
            .addOnSuccessListener { result ->
                Log.d("RAJESH",  "SUCCESS")
                Toast.makeText(this,"Success", Toast.LENGTH_SHORT).show()


            }
            .addOnFailureListener { exception ->
                Log.w("TABY", "Error getting documents.", exception)
                Toast.makeText(this,exception.localizedMessage, Toast.LENGTH_SHORT).show()
            }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.upload_coord_screen)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        buttonRecord.setOnClickListener {

            isRecording = true
            handler.post(recordRunnable)
        }

        btnStop.setOnClickListener {
            isRecording = false
            handler.removeCallbacks(recordRunnable)
        }

        buttonSubmit.setOnClickListener {
            postData()
        }
//        val extras = intent.extras
//        var value : Destination?= null
//        if (extras != null) {
//            value = extras.getSerializable("key") as Destination
//        }
//        tvDept.text = value?.dept
//        tvName.text = value?.name
//        tvLocation.text = value?.description
//        tvClassrooms.text = value?.classrooms
//        tvLabs.text = value?.labs
//        tvStudents.text = value?.students
//        tvFaculties.text = value?.faculties
//        btnNavigate.text = "Navigate " + value?.name
//
//        Glide
//            .with(applicationContext)
//            .load(value?.imageUri)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .into(ivDetails)


//        btnNavigate.setOnClickListener {
//            val intent = Intent(this, HelloGeoActivity::class.java)
//            intent.putExtra("key", value)
//            startActivity(intent)
//        }




    }
}