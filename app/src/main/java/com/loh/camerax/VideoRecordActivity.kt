package com.loh.camerax

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ScaleGestureDetector
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.databinding.DataBindingUtil
import com.loh.camerax.databinding.ActivityRecordVideoBinding
import com.loh.camerax.util.Permission
import com.loh.camerax.util.PermissionUtil
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoRecordActivity : AppCompatActivity() {

    lateinit var binding: ActivityRecordVideoBinding

    private val permissionUtil = PermissionUtil()
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private var activeRecording: ActiveRecording? = null
    private lateinit var videoCapture: VideoCapture<Recorder>

    private var camera: Camera? = null

    private val TAG = "test1233"

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_record_video)

        requestPermission()
        binding.btnStopVideo.setOnClickListener {
            stopRecord()
        }
        binding.cameraCaptureButton.setOnClickListener { startRecord() }

        binding.viewFinder.setOnTouchListener { _, motionEvent ->
            scaleGestureDetector.onTouchEvent(motionEvent)
            return@setOnTouchListener true

        }
    }

    private fun requestPermission() {
        permissionUtil.register(this) {
            if (it)
                setUp()
        }

        permissionUtil.checkSpecificPermission(Permission.Video)
    }

    private fun setUp() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDirectory = getOutputDirectory()
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setExecutor(cameraExecutor).setQualitySelector(qualitySelector)
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            try {
                // Bind use cases to camera
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, videoCapture, preview,
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))


    }

    val qualitySelector = QualitySelector
        .firstTry(QualitySelector.QUALITY_UHD)
        .thenTry(QualitySelector.QUALITY_FHD)
        .thenTry(QualitySelector.QUALITY_HD)
        .finallyTry(
            QualitySelector.QUALITY_SD,
            QualitySelector.FALLBACK_STRATEGY_LOWER
        )

    @SuppressLint("MissingPermission")
    fun startRecord() {
        val name = "CameraX-recording-" + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            this.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

// 2. Configure Recorder and Start recording to the mediaStoreOutput.
        activeRecording = videoCapture.output.prepareRecording(this, mediaStoreOutput)
            .withEventListener(ContextCompat.getMainExecutor(this), captureListener)
            .withAudioEnabled().start()
    }

    fun stopRecord() {
        val recording = activeRecording
        if (recording != null) {
            recording.stop()
            activeRecording = null
        }
    }

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status) {
            Log.d(TAG, event.toString())
        }
        //  recordingState = event

        //  updateUI(event)

        if (event is VideoRecordEvent.Finalize) {
            Log.d(TAG, event.toString())
            //showVideo(event)
        }
    }

    private val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            camera?.let {
                val scale = it.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                camera!!.cameraControl.setZoomRatio(scale)
            }
            return true
        }
    }

    val scaleGestureDetector by lazy { ScaleGestureDetector(this, listener) }
}