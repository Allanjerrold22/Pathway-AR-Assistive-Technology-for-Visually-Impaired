package com.lowjunkie.esummit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.lowjunkie.esummit.adapters.HomeAdapter
import com.lowjunkie.esummit.api.RetrofitInstance
import com.lowjunkie.esummit.models.Destination
import com.lowjunkie.esummit.models.Geometry
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var homeAdapter: HomeAdapter

    private var fireStore : FirebaseFirestore = FirebaseFirestore.getInstance()
    val TAG = "Sinamika"
    private val REQUEST_CODE_SPEECH_INPUT = 1


    suspend fun fetchCoord(destination: String)=
        RetrofitInstance.api.getPolyline(
            //    "80.045676,12.822392;$destination"
            "80.045676,12.822392;${destination}"
        )
    var points: Geometry?= null


    fun getDirection(destination: String){

        CoroutineScope(Dispatchers.IO).launch {
            val response = fetchCoord(destination)
            if(response.isSuccessful){
                points = response.body()?.routes?.get(0)?.geometry
                Log.d("Sinamike", points?.coordinates.toString())
            }
            else{
                Log.d("Sinamike", response.message())
            }
        }
    }

    var masterList: List<Destination> ?= null

    fun getData(){
        fireStore.collection("destination")
            .get()
            .addOnSuccessListener { result ->
                Log.d("TABY",  result.toString())

                val itemList : MutableList<Destination> = arrayListOf()
                for (document in result) {
                    Log.d("TABY",  document.toString())

                    itemList.add(
                        document.toObject(Destination::class.java)
                    )

                }
                masterList = itemList
                homeAdapter.differ.submitList(itemList)
            }
            .addOnFailureListener { exception ->
                Log.w("TABY", "Error getting documents.", exception)
                Toast.makeText(this,exception.localizedMessage, Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupHomeRecyclerView()
        getData()


        btnMic.setOnClickListener {
            // on below line we are calling speech recognizer intent.
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            // on below line we are passing language model
            // and model free form in our intent
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            // on below line we are passing our
            // language as a default language.
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )

            // on below line we are specifying a prompt
            // message as speak to text on below line.
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")

            // on below line we are specifying a try catch block.
            // in this block we are calling a start activity
            // for result method and passing our result code.
            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
            } catch (e: Exception) {
                // on below line we are displaying error message in toast
                Toast
                    .makeText(
                        this@MainActivity, " " + e.message,
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }



        homeAdapter.setOnItemClickListener { destination->
//           getDirection(
//               destination.coordinateY + "," + destination.coordinateX
//         )

                val intent = Intent(this, Details::class.java)
                intent.putExtra("key", destination)
                startActivity(intent)

        }

    }



    private fun setupHomeRecyclerView(){
        homeAdapter =
            HomeAdapter()
        homeRV.apply {
            adapter = homeAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }
}