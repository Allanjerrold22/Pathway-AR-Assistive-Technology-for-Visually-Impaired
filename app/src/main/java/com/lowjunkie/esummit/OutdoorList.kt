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
import kotlinx.android.synthetic.main.outdoor_list_screen.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class OutdoorList : AppCompatActivity() {
    private lateinit var homeAdapter: HomeAdapter

    private var fireStore : FirebaseFirestore = FirebaseFirestore.getInstance()
    val TAG = "Sinamika"



    var masterList: List<Destination> ?= null

    fun getData(){
        fireStore.collection("destination")
            .get()
            .addOnSuccessListener { result ->
                Log.d("TABY",  result.toString())

                val itemList : MutableList<Destination> = arrayListOf()
                for (document in result) {
                    Log.d("TABY",  document.toString())
                    val doc =   document.toObject(Destination::class.java)
                    if(doc.type != "indoor") {
                        itemList.add(
                            doc
                        )
                    }

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
        setContentView(R.layout.outdoor_list_screen)

        setupHomeRecyclerView()
        getData()


        homeAdapter.setOnItemClickListener { destination->

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
            layoutManager = LinearLayoutManager(this@OutdoorList)
        }
    }
}