package com.lowjunkie.esummit

import android.annotation.SuppressLint
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



    var masterList: List<Destination> ?= mutableListOf()
    var masterList2: List<Destination> ?= mutableListOf()


    fun getData(){
        fireStore.collection("destination")
            .get()
            .addOnSuccessListener { result ->
                Log.d("TABY",  result.toString())

                val itemList : MutableList<Destination> = arrayListOf()
                val itemList2 : MutableList<Destination> = arrayListOf()

                for (document in result) {
                    Log.d("TABY",  document.toString())

                    val doc = document.toObject(Destination::class.java)
                    if(doc.type == "indoor"){
                        itemList.add(doc)
                    }else{
                        itemList2.add(doc)
                    }


                }
                masterList = itemList + itemList2

                indoorPathTV.text = itemList.size.toString() + "Paths saved"
                outdoorPathTV.text = itemList2.size.toString() + "Paths saved"

            }
            .addOnFailureListener { exception ->
                Log.w("TABY", "Error getting documents.", exception)
                Toast.makeText(this,exception.localizedMessage, Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupHomeRecyclerView()
        getData()


        imageButtonMic.setOnClickListener {
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
//
//        btnMap.setOnClickListener {
//            val intent = Intent(this, MapActivity::class.java)
//            startActivity(intent)
//        }

//

        imageView3.setOnClickListener {
            val intent = Intent(this, IndoorList::class.java)
                startActivity(intent)
        }


        imageView2.setOnClickListener {
            val intent = Intent(this, OutdoorList::class.java)
                startActivity(intent)
        }

        btnCreateRoute.setOnClickListener {
            val intent = Intent(this, CreateRoute::class.java)
            startActivity(intent)
        }



//        homeAdapter.setOnItemClickListener { destination->
////           getDirection(
////               destination.coordinateY + "," + destination.coordinateX
////         )
//
//            if(destination.type == "indoor"){
//                val intent = Intent(this, AR::class.java)
//                intent.putExtra("key", destination)
//                startActivity(intent)
//            }else{
//                val intent = Intent(this, Details::class.java)
//                intent.putExtra("key", destination)
//                startActivity(intent)
//            }
//
//
//
//        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {

                val res: ArrayList<String> =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                // on below line we are setting data
                // to our output text view.
                masterList?.forEach { item ->
                    val firstStringOfRes = Objects.requireNonNull(res)[0]
                    val itemName = item.name

// Split the strings into words
                    val wordsInRes = firstStringOfRes.split("\\s+".toRegex())
                    val wordsInItemName = itemName.split("\\s+".toRegex())

// Check if there's any intersection between the sets of words
                    val intersection = wordsInRes.intersect(wordsInItemName)

                    if (intersection.isNotEmpty()) {
                        // Your code here

                        // Your code here


                                    if(item.type == "indoor"){
                val intent = Intent(this, AR::class.java)
                intent.putExtra("key", item)
                startActivity(intent)
            }else{
                val intent = Intent(this, Details::class.java)
                intent.putExtra("key", item)
                startActivity(intent)
            }
                    }
                }
                Log.d("DIRECT",Objects.requireNonNull(res)[0])

                //   outputTV.setText(
                //     Objects.requireNonNull(res)[0]
                // )
            }
        }
    }


    private fun setupHomeRecyclerView(){
        homeAdapter =
            HomeAdapter()
//        homeRV.apply {
//            adapter = homeAdapter
//            layoutManager = LinearLayoutManager(this@MainActivity)
//        }
    }
}