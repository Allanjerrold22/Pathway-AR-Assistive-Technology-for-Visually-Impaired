package com.lowjunkie.esummit

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.Resources
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.lowjunkie.esummit.models.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt


class AR : AppCompatActivity(), TextToSpeech.OnInitListener {

    data class EightDirection(val azimuth: Float, val name: String)

    private fun calculateDirection(objectPose0: Vector3, objectPose1: Vector3): EightDirection {
        val directionX = objectPose1.x - objectPose0.x
        val directionY = objectPose1.y - objectPose0.y
        val directionZ = objectPose1.z - objectPose0.z

        // Calculate the azimuth angle
        val azimuth = Math.toDegrees(Math.atan2(directionY.toDouble(), directionX.toDouble())).toFloat()

        // Convert the azimuth angle to a human-readable direction
        val name = when {
            azimuth in -22.5..22.5 || azimuth in (360.0 - 22.5)..360.0 -> "north"
            azimuth in 22.5..67.5 -> "northeast"
            azimuth in 67.5..112.5 -> "east"
            azimuth in 112.5..157.5 -> "southeast"
            azimuth in 157.5..202.5 -> "south"
            azimuth in 202.5..247.5 -> "southwest"
            azimuth in 247.5..292.5 -> "west"
            azimuth in 292.5..337.5 -> "northwest"
            else -> "unknown"
        }

        return EightDirection(azimuth, name)
    }


    private lateinit var arFragment: ArFragment
    var initialAnchorPosition: Vector3? = null

    private val arSceneView get() = arFragment.arSceneView
    private val scene get() = arSceneView.scene
    private var model: Renderable? = null
    lateinit var tts: TextToSpeech
    private lateinit var modelUri: String
    private var isTouched = false
    var status = 1
    var value : Destination?= null


    private fun calculateDistance(x: Float, y: Float, z: Float): Float{
        return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
    }

    private fun calculateDistance(objectPose0: Vector3, objectPose1: Vector3): Float{
        return calculateDistance(
            objectPose0.x - objectPose1.x,
            objectPose0.y - objectPose1.y,
            objectPose0.z - objectPose1.z
        )

    }


    private fun createTempImageFile(imageBytes: ByteArray): File {
        val tempFile = File.createTempFile("temp_image", ".jpg")
        FileOutputStream(tempFile).use { fos ->
            fos.write(imageBytes)
        }
        return tempFile
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.ar_screen)

        val extras = intent.extras
        if (extras != null) {
            value = extras.getSerializable("key") as Destination
        }



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

        CoroutineScope(Dispatchers.Main).launch {
            while (true){
                if(isTouched && !tts.isSpeaking){

                val coordinates = value?.coordinates?.split("$")

                for(i in coordinates?.indices!!){
                    if(i+1 == coordinates.size){
                        break
                    }
                    val coordinate = coordinates[i].split(",")

                    val dx = coordinate[0].toFloat()
                    val dy = coordinate[1].toFloat()
                    val dz = coordinate[2].toFloat()

                    if(calculateDistance(scene.camera.worldPosition, Vector3(dx, dy, dz)) < 4){
                        val coordinate = coordinates[i+1].split(",")

                        val dx = coordinate[0].toFloat()
                        val dy = coordinate[1].toFloat()
                        val dz = coordinate[2].toFloat()


                        val direction = calculateDirection(scene.camera.worldPosition, Vector3(dx, dy, dz))

                        val dist = calculateDistance(scene.camera.worldPosition, Vector3(dx, dy, dz))

                        tts.speak("Go ${direction.azimuth.toInt()} degree ${direction.name} for about ${String.format("%.1f", dist)} meters", TextToSpeech.QUEUE_FLUSH, null,"")

                        break
                    }

                }


                    val currentFrame: Frame? = arSceneView.arFrame
                    val currentImage: Image? = currentFrame?.acquireCameraImage()


                    // Convert the Image to a byte array
                    val byteBuffer: ByteBuffer = currentImage?.planes?.get(0)?.buffer ?: break
                    val imageBytes = ByteArray(byteBuffer.remaining())
                    byteBuffer.get(imageBytes)

// Save the byte array as a temporary file
                    val tempFile = createTempImageFile(imageBytes)

// Create the request body with the file
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", "image.jpg", RequestBody.create("image/jpeg".toMediaTypeOrNull(), tempFile))
                        .build()

// Create the request
                    val request = Request.Builder()
                        .url("https://fc57-2406-7400-b9-e94c-e521-238b-39cb-37c3.ngrok-free.app")
                        .post(requestBody)
                        .build()

// Create OkHttpClient and execute the request
                    val client = OkHttpClient()
                    client.newCall(request).enqueue(object : okhttp3.Callback {
                        override fun onFailure(call: okhttp3.Call, e: IOException) {
                            // Handle failure
                            e.printStackTrace()
                        }

                        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                            // Handle success
                            val responseBody = response.body?.string()
                            Log.d("TABREZ", responseBody.toString())
                            // Process the response as needed
                        }
                    })


                    currentImage?.close()


                }
                delay(2000)

            }
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



        isTouched = true

        scene.addChild(AnchorNode(anchor).apply {
            // Create the transformable model and add it to the anchor.
            val coordinates = value?.coordinates?.split("$")

            Log.d("LJJJ",coordinates.toString())
            for(i in coordinates?.indices!!){
                val coordinate = coordinates[i].split(",")

                val dx = coordinate[0].toFloat()
                val dy = coordinate[1].toFloat()
                val dz = coordinate[2].toFloat()

                Log.d("LJJJ", dx.toString())
                Log.d("LJJJ", dy.toString())
                Log.d("LJJJ", dz.toString())
                Log.d("LJJJ", i.toString())
                Log.d("LJJJ", "----")



                addChild(TransformableNode(arFragment.transformationSystem).apply {
                    renderable = model
                    renderableInstance?.animate(true)?.start()
                    worldPosition = Vector3(dx, dy, dz)
                    translationController.isEnabled = false
                    rotationController.isEnabled = false
                    scaleController.isEnabled = false
                })
            }




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