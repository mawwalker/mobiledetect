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
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.android.camera.utils.YuvToRgbConverter
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    // private lateinit var container: ConstraintLayout
    private lateinit var bitmapBuffer: Bitmap
    private var executor = Executors.newSingleThreadExecutor()
    private val permissions = listOf(Manifest.permission.CAMERA)
    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val isFrontFacing get() = lensFacing == CameraSelector.LENS_FACING_FRONT

    // private var pauseAnalysis = false
    private var imageRotationDegrees: Int = 0
    private var threshold:kotlin.Double = 0.4
    private  var nmsthreshold:kotlin.Double = 0.6
    // private val detecting = AtomicBoolean(false)
    // var detectService = Executors.newSingleThreadExecutor()
//    protected lateinit var mutableBitmap: Bitmap
//    // Correct preview output to account for display rotation
    // var rotations = floatArrayOf(0f, 90f, 180f, 270f)
//    var rotationDegrees: Float = rotations[view_finder.display.rotation]
    // var rotationDegrees: kotlin.Float = 0f
    var USE_GPU = false
    // private val tfImageBuffer = TensorImage(DataType.UINT8)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Example of a call to a native method
        // findViewById<TextView>(R.id.sample_text).text = stringFromJNI()
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
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    // .setTargetResolution(Size(1920, 1080))
                    .setTargetRotation(view_finder.display.rotation)
                    .build()

            // Set up the image analysis use case which will process frames in real time
            val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    // .setTargetResolution(Size(1920, 1080))
                    .setTargetRotation(view_finder.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

            var frameCounter = 0
            var lastFpsTimestamp = System.currentTimeMillis()
            val converter = YuvToRgbConverter(this)
            var matrix = Matrix()

            // val detectAnalyzer = DetectAnalyzer()

            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
//                if (!::bitmapBuffer.isInitialized) {
//                    // The image rotation and RGB image buffer are initialized only once
//                    // the analyzer has started running
//                    imageRotationDegrees = image.imageInfo.rotationDegrees
//                    bitmapBuffer = Bitmap.createBitmap(
//                            image.width, image.height, Bitmap.Config.ARGB_8888)
//                }
//                // Convert the image to RGB and place it in our shared buffer
//                image.use { converter.yuvToRgb(image.image!!, bitmapBuffer) }

                imageRotationDegrees = image.imageInfo.rotationDegrees
                var bitmapsrc: Bitmap = imageToBitmap(image) // 格式转换
                var matrix = Matrix()
                // matrix = getCorrectionMatrix(image, view_finder)
                matrix.postRotate(imageRotationDegrees.toFloat())
//                var width = bitmapsrc.width
//                var height = bitmapsrc.height
                var width = image.width
                var height = image.height
//                var width = 640
//                var height = 480
                // bitmapBuffer = Bitmap.createBitmap(bitmapsrc, 0, 0, width.coerceAtMost(height), width.coerceAtMost(height), matrix, false)
                bitmapBuffer = Bitmap.createBitmap(bitmapsrc, 0, 0, width, height, matrix, false)
                // bitmapBuffer = bitmapsrc

                // YOLOv4.init(assets, USE_GPU)
                NanoDet.init(assets, USE_GPU)
                // val predictions:Array<Box> = YOLOv4.detect(bitmapBuffer, threshold, nmsthreshold)
                val predictions: Array<Box> = NanoDet.detect(bitmapBuffer, threshold, nmsthreshold)
                box_prediction.post {
                    box_prediction.drawPredictionBox(predictions)
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

    private fun getCorrectionMatrix(imageProxy: ImageProxy, previewView: PreviewView) : Matrix {
        val cropRect = imageProxy.cropRect
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val matrix = Matrix()

        // A float array of the source vertices (crop rect) in clockwise order.
        val source = floatArrayOf(
                cropRect.left.toFloat(),
                cropRect.top.toFloat(),
                cropRect.right.toFloat(),
                cropRect.top.toFloat(),
                cropRect.right.toFloat(),
                cropRect.bottom.toFloat(),
                cropRect.left.toFloat(),
                cropRect.bottom.toFloat()
        )

        // A float array of the destination vertices in clockwise order.
        val destination = floatArrayOf(
                0f,
                0f,
                previewView.width.toFloat(),
                0f,
                previewView.width.toFloat(),
                previewView.height.toFloat(),
                0f,
                previewView.height.toFloat()
        )

        // The destination vertexes need to be shifted based on rotation degrees. The
        // rotation degree represents the clockwise rotation needed to correct the image.

        // Each vertex is represented by 2 float numbers in the vertices array.
        val vertexSize = 2
        // The destination needs to be shifted 1 vertex for every 90° rotation.
        val shiftOffset = rotationDegrees / 90 * vertexSize;
        val tempArray = destination.clone()
        for (toIndex in source.indices) {
            val fromIndex = (toIndex + shiftOffset) % source.size
            destination[toIndex] = tempArray[fromIndex]
        }
        matrix.setPolyToPoly(source, 0, destination, 0, 4)
        return matrix
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
//    external fun stringFromJNI(): String
//
//    companion object {
//        // Used to load the 'native-lib' library on application startup.
//        init {
//            System.loadLibrary("detect")
//        }
//    }
}