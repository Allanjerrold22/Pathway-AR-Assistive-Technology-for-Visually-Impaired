package com.lowjunkie.esummit

import android.location.Location
import android.location.LocationManager
import android.opengl.Matrix
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Anchor
import com.google.ar.core.Earth
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.firebase.firestore.FirebaseFirestore
import com.lowjunkie.esummit.api.RetrofitInstance
import com.lowjunkie.esummit.java.common.helpers.DisplayRotationHelper
import com.lowjunkie.esummit.java.common.helpers.TrackingStateHelper
import com.lowjunkie.esummit.java.common.samplerender.*
import com.lowjunkie.esummit.java.common.samplerender.arcore.BackgroundRenderer
import com.lowjunkie.esummit.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.util.*


class HelloGeoRenderer(val activity: HelloGeoActivity
                       , val destination: Destination
//, val value: Geometry?
) :
    SampleRender.Renderer, DefaultLifecycleObserver, TextToSpeech.OnInitListener {
    //<editor-fold desc="ARCore initialization" defaultstate="collapsed">


    private var fireStore : FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        val TAG = "HelloGeoRenderer"
        private var tts: TextToSpeech? = null

        private val Z_NEAR = 0.1f
        private val Z_FAR = 1000f
    }

    lateinit var backgroundRenderer: BackgroundRenderer
    lateinit var virtualSceneFramebuffer: Framebuffer
    var hasSetTextureNames = false

    // Virtual object (ARCore pawn)
    lateinit var virtualObjectMesh: Mesh
    lateinit var virtualObjectMesh2: Mesh

    lateinit var virtualObjectShader: Shader
    lateinit var virtualObjectTexture: Texture

    lateinit var virtualObjectShader2: Shader
    lateinit var virtualObjectTexture2: Texture

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    val modelMatrix = FloatArray(16)
    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val modelViewMatrix = FloatArray(16) // view x model

    val modelViewProjectionMatrix = FloatArray(16) // projection x view x model

    val session
        get() = activity.arCoreSessionHelper.session

    val displayRotationHelper = DisplayRotationHelper(activity)
    val trackingStateHelper = TrackingStateHelper(activity)




    suspend fun fetchCoord(origin: String): Response<Direction> {

        val poly = origin+";"+destination.coordinateY + "," + destination.coordinateX
        return RetrofitInstance.api.getPolyline(
            //    "80.045676,12.822392;$destination"
            poly
        )
    }
    var points: Geometry?= null






    override fun onResume(owner: LifecycleOwner) {
        displayRotationHelper.onResume()
        hasSetTextureNames = false
    }

    var model : ModelRenderable ?= null
    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }

    override fun onSurfaceCreated(render: SampleRender) {
        // Prepare the rendering objects.
        // This involves reading shaders and 3D model files, so may throw an IOException.
        try {
            tts = TextToSpeech(activity.applicationContext, this)

            backgroundRenderer = BackgroundRenderer(render)
            virtualSceneFramebuffer = Framebuffer(render, /*width=*/ 1, /*height=*/ 1)

            // Virtual object to render (Geospatial Marker)
            virtualObjectTexture =
                Texture.createFromAsset(
                    render,
                    "models/balloon-baked.png",
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    Texture.ColorFormat.SRGB
                )

            virtualObjectTexture2 =
                Texture.createFromAsset(
                    render,
                    "models/nb.jpg",
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    Texture.ColorFormat.SRGB
                )




            virtualObjectMesh = Mesh.createFromAsset(render, "models/Balloon-2.obj");
            virtualObjectMesh2 = Mesh.createFromAsset(render, "models/Notice-board.obj");

            // virtualObjectMesh = Mesh.createFromAsset(render, "models/arrow_new.glb");
            virtualObjectShader =
                Shader.createFromAssets(
                    render,
                    "shaders/ar_unlit_object.vert",
                    "shaders/ar_unlit_object.frag",
                    /*defines=*/ null)
                    .setTexture("u_Texture", virtualObjectTexture)

            virtualObjectShader2 =
                Shader.createFromAssets(
                    render,
                    "shaders/ar_unlit_object.vert",
                    "shaders/ar_unlit_object.frag",
                    /*defines=*/ null)
                    .setTexture("u_Texture", virtualObjectTexture2)

            backgroundRenderer.setUseDepthVisualization(render, false)
            backgroundRenderer.setUseOcclusion(render, false)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read a required asset file", e)
            showError("Failed to read a required asset file: $e")
        }
    }

    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        virtualSceneFramebuffer.resize(width, height)
    }
    //</editor-fold>
    var start = 0
    override fun onDrawFrame(render: SampleRender) {
        val session = session ?: return
        val earthSession: Earth? = session.earth
        var tempStep : Step ?= null
        for(i in start until (steps?.size ?: 0)){
            val step = steps!![i]
            if(String.format("%.3f", step.maneuver.location[0]).toDouble() === String.format("%.3f", earthSession?.cameraGeospatialPose?.longitude).toDouble()
                && String.format("%.3f", step.maneuver.location[1]).toDouble() === String.format("%.3f", earthSession?.cameraGeospatialPose?.latitude).toDouble()
            ){
                tts!!.speak(step.maneuver.instruction, TextToSpeech.QUEUE_FLUSH, null,"")
                start+=1
                Log.d("DirectionsTTS",step.maneuver.instruction)
            }
        }
//    steps?.forEach { step->
//
//    }
        //<editor-fold desc="ARCore frame boilerplate" defaultstate="collapsed">
        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
            session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
            hasSetTextureNames = true
        }

        // -- Update per-frame state

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session)

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        val frame =
            try {
                session.update()
            } catch (e: CameraNotAvailableException) {
                Log.e(TAG, "Camera not available during onDrawFrame", e)
                showError("Camera not available. Try restarting the app.")
                return
            }

        val camera = frame.camera

        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundRenderer.updateDisplayGeometry(frame)

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

        // -- Draw background
        if (frame.timestamp != 0L) {
            // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
            // drawing possible leftover data from previous sessions if the texture is reused.
            backgroundRenderer.drawBackground(render)
        }

        // If not tracking, don't draw 3D objects.
        if (camera.trackingState == TrackingState.PAUSED) {
            return
        }

        // Get projection matrix.
        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)

        // Get camera matrix and draw.
        camera.getViewMatrix(viewMatrix, 0)

        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)
        //</editor-fold>

        // TODO: Obtain Geospatial information and display it on the map.
        val earth = session.earth
        if (earth?.trackingState == TrackingState.TRACKING) {
            // TODO: the Earth object may be used here.
            val cameraGeospatialPose = earth.cameraGeospatialPose

            activity.view.mapView?.updateMapPosition(
                latitude = cameraGeospatialPose.latitude,
                longitude = cameraGeospatialPose.longitude,
                heading = cameraGeospatialPose.heading
            )
        }
        earth?.let { activity.view.updateStatusText(it, earth.cameraGeospatialPose) }

        // Draw the placed anchor, if it exists.
//    earthAnchor?.let {
//      render.renderCompassAtAnchor(it)
//    }

        earthAnchors.forEach { anchor ->
            anchor.let {
                render.renderCompassAtAnchor(it!!)
            }
        }

        earthAnchors2.forEach { anchor ->
            anchor.let {
                render.renderCompassAtAnchor2(it!!)
            }
        }

        // Compose the virtual scene with the background.
        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)
    }

    var earthAnchor: Anchor? = null
    var earthAnchors: List<Anchor?> = listOf()
    var earthAnchors2: MutableList<Anchor?> = mutableListOf()

    var steps : MutableList<Step> ?= null
    fun onMapClick(
        latLng: LatLng
    ) {
        val earth = session?.earth ?: return
        if (earth.trackingState != TrackingState.TRACKING) {
            return
        }
        earthAnchor?.detach()
        earthAnchors.forEach {
            it?.detach()
        }
        earthAnchors2.forEach{
            it?.detach()
        }

        val altitude = earth.cameraGeospatialPose.altitude - 2.5


        val qx = 0f
        val qy = 0f
        val qz = 0f
        val qw = 1f

        var lis = mutableListOf<Anchor>()


        fun getEvents() {
            fireStore.collection("events")
                .get()
                .addOnSuccessListener { result ->
                    Log.d("TABY",  result.toString())

                    val itemList : MutableList<Destination> = arrayListOf()
                    for (document in result) {
                        Log.d("TABY",  document.toString())


                        val doc =   document.toObject(Event::class.java)

                        earthAnchors2.add(earth.createAnchor(doc.coordinateX.toDouble() , doc.coordinateY.toDouble(),
                            altitude , qx, qy, qz, qw))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("TABY", "Error getting documents.", exception)
                    Toast.makeText(activity.applicationContext,exception.localizedMessage, Toast.LENGTH_SHORT).show()
                }


        }

        getEvents()

        CoroutineScope(Dispatchers.IO).launch {
            if(destination.type == "indoor"){

//
//                val coordinates = destination?.coordinates
//                withContext(Dispatchers.Main){
//                    activity.view.drawPoly(coordinates!!)
//                }
//
//                for(i in 1 until coordinates!!.size){
//                    val point_A = coordinates[i-1]
//                    val point_B = coordinates[i]
//
//                    var targetlocation = Location(LocationManager.GPS_PROVIDER)
//                    targetlocation.latitude = point_A[1]
//                    targetlocation.longitude = point_A[0]
//
//                    var targetlocation2 = Location(LocationManager.GPS_PROVIDER)
//                    targetlocation2.latitude = point_B[1]
//                    targetlocation2.longitude = point_B[0]
//
//
//
////-------------ONLY HOTSPOTS
//
//                    Log.d("Jerrold",point_A[1].toString() +"," + point_A[0].toString())
////          lis.add(earth.createAnchor(point_A[1] , point_A[0],
////            altitude , qx, qy, qz, qw))
//// -------------ONLY HOTSPOTS
//
//                    val diff_X: Double = point_B[0] - point_A[0]
//                    val diff_Y: Double = point_B[1] - point_A[1]
//                    val pointNum = 8
//
//                    val intervalX: Double = diff_X / (pointNum + 1)
//                    val intervalY: Double = diff_Y / (pointNum + 1)
//
//                    for(i in 0 until pointNum){
//                        Log.d("Kongarasan",(point_A[1] + intervalY * (i)).toString() + ","+ (point_A[0] + intervalX*(i)).toString())
//
//                    }
//                    Log.d("Kongarasan","----------------------------")
//
//                    //-------------UNDO THIS LATER PLS
//                    for(i in 0 until pointNum){
//
//                        lis.add(earth.createAnchor(
//                            point_A[1] + intervalY * (i), point_A[0] + intervalX*(i),
//                            altitude , qx, qy, qz, qw)
//                        )
//                    }
//                    //-------------UNDO THIS LATER PLS
////          lis.add(earth.createAnchor(
////            12.823706, 80.043635,
////            altitude , qx, qy, qz, qw))
//                }
//                earthAnchors = lis


            }
            else{
                val response = fetchCoord(earth?.cameraGeospatialPose?.longitude.toString() +","+earth?.cameraGeospatialPose?.latitude)
                if(response.isSuccessful){
                    points = response.body()?.routes?.get(0)?.geometry
                    val coordinates = points?.coordinates
                    steps = response.body()?.routes?.get(0)?.legs?.get(0)?.steps as MutableList<Step>?
                    withContext(Dispatchers.Main){
                        activity.view.drawPoly(coordinates!!)
                    }

                    for(i in 1 until coordinates!!.size){
                        val point_A = coordinates[i-1]
                        val point_B = coordinates[i]

                        var targetlocation = Location(LocationManager.GPS_PROVIDER)
                        targetlocation.latitude = point_A[1]
                        targetlocation.longitude = point_A[0]

                        var targetlocation2 = Location(LocationManager.GPS_PROVIDER)
                        targetlocation2.latitude = point_B[1]
                        targetlocation2.longitude = point_B[0]



//-------------ONLY HOTSPOTS

                        Log.d("Jerrold",point_A[1].toString() +"," + point_A[0].toString())
//          lis.add(earth.createAnchor(point_A[1] , point_A[0],
//            altitude , qx, qy, qz, qw))
// -------------ONLY HOTSPOTS

                        val diff_X: Double = point_B[0] - point_A[0]
                        val diff_Y: Double = point_B[1] - point_A[1]
                        val pointNum = 8

                        val intervalX: Double = diff_X / (pointNum + 1)
                        val intervalY: Double = diff_Y / (pointNum + 1)

                        for(i in 0 until pointNum){
                            Log.d("Kongarasan",(point_A[1] + intervalY * (i)).toString() + ","+ (point_A[0] + intervalX*(i)).toString())

                        }
                        Log.d("Kongarasan","----------------------------")

                        //-------------UNDO THIS LATER PLS
                        for(i in 0 until pointNum){

                            lis.add(earth.createAnchor(
                                point_A[1] + intervalY * (i), point_A[0] + intervalX*(i),
                                altitude , qx, qy, qz, qw)
                            )
                        }
                        //-------------UNDO THIS LATER PLS
//          lis.add(earth.createAnchor(
//            12.823706, 80.043635,
//            altitude , qx, qy, qz, qw))
                    }
                    earthAnchors = lis

                }
                else{
                    Log.d("DirectionAPI", response.message())
                }
            }





        }




    }

    private fun SampleRender.renderCompassAtAnchor(anchor: Anchor) {
        // Get the current pose of the Anchor in world space. The Anchor pose is updated
        // during calls to session.update() as ARCore refines its estimate of the world.
        anchor.pose.toMatrix(modelMatrix, 0)

        // Calculate model/view/projection matrices
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        // Update shader properties and draw

        virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)

        draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer)
    }

    private fun SampleRender.renderCompassAtAnchor2(anchor: Anchor) {
        // Get the current pose of the Anchor in world space. The Anchor pose is updated
        // during calls to session.update() as ARCore refines its estimate of the world.
        anchor.pose.toMatrix(modelMatrix, 0)

        // Calculate model/view/projection matrices
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        // Update shader properties and draw

        virtualObjectShader2.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)

        draw(virtualObjectMesh2, virtualObjectShader2, virtualSceneFramebuffer)
    }

    private fun showError(errorMessage: String) =
        activity.view.snackbarHelper.showError(activity, errorMessage)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language not supported!")
            } else {
            }
        }
    }
}