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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.lowjunkie.esummit.R
import java.lang.Exception
import java.lang.Math.abs
import java.util.*


class AR : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var arFragment: ArFragment
    var initialAnchorPosition: Vector3? = null

    private val arSceneView get() = arFragment.arSceneView
    private val scene get() = arSceneView.scene
    private var model: Renderable? = null
    lateinit var tts: TextToSpeech
    private lateinit var modelUri: String
    var status = 1

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


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.ar_screen)
        val intent = this.intent
        val bundle = intent.getBundleExtra("item")
        tts = TextToSpeech(this, this)
        tts.setSpeechRate(1.2f)





        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        arFragment = (supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment).apply {
            setOnSessionConfigurationListener { session, config ->
                instructionsController = null
            }

            setOnTapArPlaneListener(::onTapPlane)
        }

        CoroutineScope(Dispatchers.Main).launch {
            loadModels()
        }







    }

    private fun loadModels() {
        try{

            ModelRenderable.builder()
                .setSource(applicationContext, Uri.parse("https://firebasestorage.googleapis.com/v0/b/roomer-ca0e9.appspot.com/o/Chair%20models%2FChair-1.glb?alt=media&token=70f24427-aac1-4804-a49f-9e594caa6a74"))
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
        val anchor = hitResult.createAnchor()

        if (initialAnchorPosition == null) {
            initialAnchorPosition = Vector3(anchor.pose.tx(), anchor.pose.ty(), anchor.pose.tz())
        }else{
            val currentOffset = calculateOffsetFromInitialPosition(Vector3(anchor.pose.tx(), anchor.pose.ty(), anchor.pose.tz()))
            Log.d("Allan", currentOffset.toString())

        }

        scene.addChild(AnchorNode(anchor).apply {
            // Create the transformable model and add it to the anchor.

            addChild(TransformableNode(arFragment.transformationSystem).apply {
                renderable = model
                renderableInstance?.animate(true)?.start()
                worldPosition = Vector3(0f,0f,0f)
            })

            addChild(TransformableNode(arFragment.transformationSystem).apply {
                renderable = model
                renderableInstance?.animate(true)?.start()
                worldPosition = Vector3(1.2421134F, (-0.049562514).toFloat(), 1.008232F)
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