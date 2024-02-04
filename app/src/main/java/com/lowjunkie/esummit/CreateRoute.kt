package com.lowjunkie.esummit

import android.R.array
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.CameraStream
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.lowjunkie.esummit.R
import com.lowjunkie.esummit.models.Destination
//import kotlinx.android.synthetic.main.activity_main.btnUpload
import kotlinx.android.synthetic.main.create_route_screen.arNameET
import kotlinx.android.synthetic.main.create_route_screen.btnARUpload
import kotlinx.android.synthetic.main.upload_coord_screen.coordNameET
import java.lang.Exception
import java.lang.Math.abs
import java.util.*


class CreateRoute : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var arFragment: ArFragment
    var initialAnchorPosition: Vector3? = null
    private var fireStore : FirebaseFirestore = FirebaseFirestore.getInstance()

    private val arSceneView get() = arFragment.arSceneView
    private val scene get() = arSceneView.scene
    private var model: Renderable? = null
    lateinit var tts: TextToSpeech
    private lateinit var modelUri: String
    private var isTouched = false
    private var coordString = ""
    var value : Destination?= null

    fun calculateOffsetFromInitialPosition(currentAnchorPosition: Vector3): String {
        if (initialAnchorPosition != null) {
            // Calculate the distance between the initial position and the current position
            val dx = currentAnchorPosition.x - initialAnchorPosition!!.x
            val dy = currentAnchorPosition.y - initialAnchorPosition!!.y
            val dz = currentAnchorPosition.z - initialAnchorPosition!!.z

            // Calculate the total offset distance
            val offset = "${dx}, ${dy}, ${dz}"

            // Return the offset distance in meters
            return offset
        }

        // If initialAnchorPosition is not set, return 0
        return ""
    }

    fun postData(){

        coordString = coordString.dropLast(1)

        fireStore.collection("destination").document(arNameET.text.toString()).set(
            mutableMapOf(
                "type" to "indoor",
                "name" to arNameET.text.toString(),
                "coordinates" to coordString
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


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.create_route_screen)

        val extras = intent.extras
        if (extras != null) {
            value = extras.getSerializable("key") as Destination
        }



        tts = TextToSpeech(this, this)
        tts.setSpeechRate(1.2f)





        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        arFragment = (supportFragmentManager.findFragmentById(R.id.arFragment2) as ArFragment).apply {
            setOnSessionConfigurationListener { session, config ->
                instructionsController = null
            }

            setOnTapArPlaneListener(::onTapPlane)
        }

        CoroutineScope(Dispatchers.Main).launch {
            loadModels()
        }


        btnARUpload.setOnClickListener {
            postData()
        }





    }

    private fun loadModels() {
        try{

            ModelRenderable.builder()
                .setSource(applicationContext, Uri.parse("https://firebasestorage.googleapis.com/v0/b/indoornavigator-1fb19.appspot.com/o/dragon_ball.glb?alt=media&token=87667bab-e4dd-49c3-9396-bdd28c8785d9"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept {
                    model = it
                }
        }
        catch (e:Exception){
            //Toast.makeText(this,e.localizedMessage.toString(),Toast.LENGTH_SHORT).show()
        }

    }

    private fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {

        if (model == null) {
            Toast.makeText(applicationContext, "Loading...", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the Anchor.
        if(isTouched){
            return
        }
        val anchor = hitResult.createAnchor()

        if (initialAnchorPosition == null) {
            initialAnchorPosition = Vector3(anchor.pose.tx(), anchor.pose.ty(), anchor.pose.tz())
            coordString = "${anchor.pose.tx()}, ${anchor.pose.ty()}, ${anchor.pose.tz()}$"
        }else{
            val currentOffset = calculateOffsetFromInitialPosition(Vector3(anchor.pose.tx(), anchor.pose.ty(), anchor.pose.tz()))
            coordString += currentOffset
            coordString += "$"
            Log.d("Allan", currentOffset.toString())
        }

        scene.addChild(AnchorNode(anchor).apply {
            // Create the transformable model and add it to the anchor.
            addChild(TransformableNode(arFragment.transformationSystem).apply {
                renderable = model
                renderableInstance?.animate(true)?.start()
                worldPosition = Vector3(0f,0f,0f)
            })




        })
    }








    fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e("TAG", "Sceneform requires Android N or later")
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        val openGlVersionString =
            (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion

        return true
    }

    override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Unknown error occured")
            }



        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }

    val Int.px get() = (this * Resources.getSystem().displayMetrics.density).toInt()

}