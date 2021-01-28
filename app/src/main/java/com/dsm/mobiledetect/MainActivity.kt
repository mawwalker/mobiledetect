package com.dsm.mobiledetect

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Size
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var bitmapBuffer: Bitmap
    private var executor = Executors.newSingleThreadExecutor()
    private val permissions = listOf(Manifest.permission.CAMERA)
    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var imageRotationDegrees: Int = 0
    private var threshold:Double = 0.1
    private  var nmsthreshold:Double = 0.9
    private var nanoDet = 1
    private var yoloV5s = 2
    private var yoloV4Tiny = 3
    private var useModel: Int = nanoDet

    private var useGPU = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        // Request permissions each time the app resumes, since they can be revoked at any time
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(
                    this, permissions.toTypedArray(), permissionsRequestCode)
        } else {
            bindCameraUseCases()
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && hasPermissions(this)) {
            bindCameraUseCases()
        } else {
            finish() // If we don't have the required permissions, we can't run
        }
    }

    /** Convenience method used to check if all permissions required by this app are granted */
    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindCameraUseCases() = view_finder.post {
        startCamera()
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {

            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            // Set up the view finder use case to display camera preview
            val preview = Preview.Builder()
                    // .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setTargetResolution(Size(1080, 1440))
                    .setTargetRotation(view_finder.display.rotation)
                    .build()

            // Set up the image analysis use case which will process frames in real time
            val imageAnalysis = ImageAnalysis.Builder()
                    // .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setTargetResolution(Size(1080, 1440))
                    .setTargetRotation(view_finder.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
                imageRotationDegrees = image.imageInfo.rotationDegrees
                var bitmapsrc: Bitmap = imageToBitmap(image) // imageproxy to bitmap
                var matrix = Matrix()
                matrix.postRotate(imageRotationDegrees.toFloat())
                var width = image.width
                var height = image.height
                bitmapBuffer = Bitmap.createBitmap(bitmapsrc, 0, 0, width, height, matrix, false)
                // bitmapBuffer = bitmapsrc

                lateinit var predictions:Array<Box>

                if (useModel == yoloV5s) {
                    YOLOv5s.init(assets, useGPU)
                    predictions = YOLOv5s.detect(bitmapBuffer, threshold, nmsthreshold)
                } else if (useModel == yoloV4Tiny) {
                    YOLOv4.init(assets, useGPU)
                    predictions = YOLOv4.detect(bitmapBuffer, threshold, nmsthreshold)
                } else if (useModel == nanoDet) {
                    threshold = 0.4
                    nmsthreshold = 0.6
                    NanoDet.init(assets, useGPU)
                    predictions = NanoDet.detect(bitmapBuffer, threshold, nmsthreshold)
                }

                box_prediction.post {
                    box_prediction.drawPredictionBox(predictions, image.width, image.height)
                    box_prediction.visibility = View.VISIBLE
                }

            })

            // Create a new camera selector each time, enforcing lens facing
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            // Apply declared configs to CameraX using the same lifecycle owner
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageAnalysis)

            // Use the camera object to link our preview use case with the view
            preview.setSurfaceProvider(view_finder.surfaceProvider)

        }, ContextCompat.getMainExecutor(this))
    }

    private fun imageToBitmap(image: ImageProxy): Bitmap {
        val nv21 = imageToNV21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        image.close()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun imageToNV21(image: ImageProxy): ByteArray {
        val planes = image.planes
        val y = planes[0]
        val u = planes[1]
        val v = planes[2]
        val yBuffer = y.buffer
        val uBuffer = u.buffer
        val vBuffer = v.buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        // U and V are swapped
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]
        return nv21
    }

}