package com.lowjunkie.esummit

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.Resources
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
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
import com.google.gson.Gson
import com.lowjunkie.esummit.models.Destination
import com.lowjunkie.esummit.models.ImageCaption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
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
        var azimuth = Math.toDegrees(Math.atan2(directionY.toDouble(), directionX.toDouble())).toFloat()
        azimuth = ((azimuth % 360.0 + 360.0) % 360.0).toFloat()

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

                    if(calculateDistance(scene.camera.worldPosition, Vector3(dx, dy, dz)) < 3){
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





                }
                delay(2000)

            }
        }


        CoroutineScope(Dispatchers.Main).launch {
            while (true){


                        val currentFrame: Frame? = arSceneView.arFrame
                        val currentImage: Image? = currentFrame?.acquireCameraImage()
                        Log.d("Tabrez", currentImage?.format.toString())

                        val width = currentImage?.width
                        val height = currentImage?.height

                        // Convert the YUV_420_888 image to NV21 format byte array
                        val yuvBytes = ByteArray(width!! * height!! * 3 / 2)

                        // Get the YUV planes
                        val planes = currentImage!!.planes
                        var buffer: ByteBuffer

                        // Process each YUV plane
                        for (i in 0 until planes.size) {
                            buffer = planes[i].buffer
                            val remaining = buffer.remaining()

                            // Copy the data to the yuvBytes array
                            buffer.get(yuvBytes, yuvBytes.size - remaining, remaining)
                        }

                        // Create a YuvImage from the NV21 data
                        val yuvImage = YuvImage(yuvBytes, ImageFormat.NV21, width!!, height!!, null)

                        // Create a rectangle representing the entire image
                        val rect = Rect(0, 0, width, height)

                        // Create a ByteArrayOutputStream to hold the JPEG data
                        val outputStream = ByteArrayOutputStream()

                        // Compress the YuvImage to JPEG format with a quality of 90 (adjust as needed)
                        yuvImage.compressToJpeg(rect, 90, outputStream)

                        // Convert the ByteArrayOutputStream to a ByteArray
                        val jpegByteArray = outputStream.toByteArray()

                        val tempFile = createTempImageFile(jpegByteArray)

                        currentImage.close()


                        val requestBody = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart(
                                "image", "image.JPG",
                                tempFile.asRequestBody("image/jpeg".toMediaType())

                            )
                            .build()

// Create the request
                        val request = Request.Builder()
                            .url("https://1b82-2406-7400-b9-e94c-28a7-22a8-30e5-e42e.ngrok-free.app/detect")
                            .post(requestBody)
                            .build()

// Create OkHttpClient and execute the request
                        val client = OkHttpClient()
                        client.newCall(request).enqueue(object : okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: IOException) {
                                // Handle failure

                                e.printStackTrace()
                            }

                            override fun onResponse(
                                call: okhttp3.Call,
                                response: okhttp3.Response
                            ) {
                                // Handle success
                                val responseBody = response.body
                                val res = Gson().fromJson(
                                    responseBody?.string(),
                                    ImageCaption::class.java
                                )
                                Log.d("TABREZ", res.result)
                                tts.speak(res.result, TextToSpeech.QUEUE_FLUSH, null, "")

                                // Process the response as needed
                            }
                        })

                delay(8000)

            }
        }

    }




    private fun loadModels() {
        try{

            ModelRenderable.builder()
                .setSource(applicationContext, Uri.parse("https://firebasestorage.googleapis.com/v0/b/indoornavigator-1fb19.appspot.com/o/little_duck.glb?alt=media&token=5149ab74-3424-4264-ab21-7e1bf634dab8"))
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
                    localScale = Vector3(0.2F,0.2F,0.2F)

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