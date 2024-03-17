package com.npci.selfivideoprocessingapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.media.MediaExtractor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.npci.selfivideoprocessingapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isRecording = false
    private var startTime: Long = 0
    private var timerHandler: Handler = Handler()
    private lateinit var timerRunnable: Runnable

    private lateinit var mediaCodec: MediaCodec
    private lateinit var mediaExtractor: MediaExtractor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Set up the listeners for video capture buttons
        viewBinding.videoCaptureButton.setOnClickListener { toggleRecording() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    // Implements VideoCapture use case, including start and stop capturing.
    private fun startRecording() {
        val videoCapture = this.videoCapture ?: return
        isRecording = true

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        recording = videoCapture.output.prepareRecording(this, mediaStoreOutputOptions).apply {
            if (PermissionChecker.checkSelfPermission(
                    this@MainActivity, Manifest.permission.RECORD_AUDIO
                ) == PermissionChecker.PERMISSION_GRANTED
            ) {
                withAudioEnabled()
            }
        }.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    // Start timer
                    startTime = System.currentTimeMillis()
                    startTimer()
                    updateUIForRecording(true)
                }
                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        val msg =
                            "Video capture succeeded: " + "${recordEvent.outputResults.outputUri}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, msg)
                    } else {
                        recording?.close()
                        recording = null
                        Log.e(
                            TAG,
                            "Video capture failed: ${recordEvent.error.toString()}",
//                            recordEvent.error.toString()
                        )
                    }
                    // Stop timer
                    stopTimer()
                    updateUIForRecording(false)
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            // Image capture use case
            imageCapture = ImageCapture.Builder().build()

            // Video capture use case
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle Permission granted/rejected
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && it.value == false) permissionGranted = false
        }
        if (!permissionGranted) {
            Toast.makeText(
                baseContext, "Permission request denied", Toast.LENGTH_SHORT
            ).show()
        } else {
            startCamera()
        }
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun toggleRecording() {
        if (isRecording) {
            // Stop recording
            stopRecording()
        } else {
            // Start recording
            startRecording()
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        isRecording = false
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                val millis = System.currentTimeMillis() - startTime
                val seconds = (millis / 1000).toInt() % 60
                val minutes = (millis / (1000 * 60)).toInt() % 60
                val timeStr = String.format("%02d:%02d", minutes, seconds)
                viewBinding.timerTextView.text = timeStr

                if (millis < 30000) { // 30 seconds
                    timerHandler.postDelayed(this, 500) // Update every 500 milliseconds
                } else {
                    stopRecording()
                    Toast.makeText(
                        baseContext,
                        "Maximum recording time reached (30 seconds)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        timerHandler.post(timerRunnable)
    }

    private fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updateUIForRecording(isRecording: Boolean) {
        if (isRecording) {
            viewBinding.videoCaptureButton.text = getString(R.string.stop_capture)
        } else {
            viewBinding.videoCaptureButton.text = getString(R.string.start_capture)
        }
    }
}
