package com.lowjunkie.esummit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.lowjunkie.esummit.models.Destination
import com.lowjunkie.esummit.models.Geometry
import kotlinx.android.synthetic.main.card_item.view.*
import kotlinx.android.synthetic.main.details_screen.*

class Details: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.details_screen)
        val extras = intent.extras
        var value : Destination?= null
        if (extras != null) {
            value = extras.getSerializable("key") as Destination
        }
        tvDept.text = value?.dept
        tvName.text = value?.name
        tvLocation.text = value?.description
        tvClassrooms.text = value?.classrooms
        tvLabs.text = value?.labs
        tvStudents.text = value?.students
        tvFaculties.text = value?.faculties
        btnNavigate.text = "Navigate " + value?.name

        Glide
            .with(applicationContext)
            .load(value?.imageUri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(ivDetails)


        btnNavigate.setOnClickListener {
            val intent = Intent(this, HelloGeoActivity::class.java)
            intent.putExtra("key", value)
            startActivity(intent)
        }




    }
}