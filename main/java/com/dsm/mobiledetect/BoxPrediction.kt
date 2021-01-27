package com.dsm.mobiledetect

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.util.*
import android.graphics.RectF
import androidx.camera.core.CameraSelector
import kotlinx.android.synthetic.main.activity_main.view.*

class BoxPrediction constructor(context: Context?, attributeSet: AttributeSet?) :
    View(context, attributeSet) {

    // private val resultRectF: MutableList<RectF> = mutableListOf()
    var results:Array<Box> = emptyArray()
    private var boxPaint = Paint().apply {
        alpha = 200
        style = Paint.Style.STROKE
        strokeWidth = 10f
        textSize = 80f
        // color = ContextCompat.getColor(context!!, android.R.color.black)
        // strokeWidth = 10f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (box in results) {
            boxPaint.color = box.color
            boxPaint.style = Paint.Style.FILL
            canvas.drawText(
                    box.getLabel() + java.lang.String.format(
                            Locale.CHINESE,
                            " %.3f",
                            box.score
                    ), box.x0 + 3, box.y0 + 80f, boxPaint
            )
//            var viewRect = RectF(view_finder.width * box.x0 / 320,
//                    view_finder.height * box.y0 / 320, view_finder.width * box.x1 / 320,
//                    view_finder.height * box.y1 / 320)
            // var viewRect = RectF(0f, 0f, 100f, 100f)
            boxPaint.style = Paint.Style.STROKE
            canvas.drawRect(box.rect, boxPaint)
            // canvas.drawRect(viewRect, boxPaint)

        }
        // resultRectF.forEach { canvas.drawRect(it, boxPaint) }
    }

    fun drawPredictionBox(predictions: Array<Box>) {
        // val resultRectF = predictionsToRectF(predictions)
        this.results = predictions
        // this.resultRectF.clear()
        // this.resultRectF.addAll(resultRectF)
        invalidate()
    }


//    private fun mapOutputCoordinates(location: RectF): RectF {
//
//        // Step 1: map location to the preview coordinates
//        val previewLocation = RectF(
//                location.left * view_finder.width,
//                location.top * view_finder.height,
//                location.right * view_finder.width,
//                location.bottom * view_finder.height
//        )
//
//        // Step 2: compensate for camera sensor orientation and mirroring
//        val isFrontFacing = lensFacing == CameraSelector.LENS_FACING_FRONT
//        val correctedLocation = if (isFrontFacing) {
//            RectF(
//                    view_finder.width - previewLocation.right,
//                    previewLocation.top,
//                    view_finder.width - previewLocation.left,
//                    previewLocation.bottom)
//        } else {
//            previewLocation
//        }
//
//        // Step 3: compensate for 1:1 to 4:3 aspect ratio conversion + small margin
//        val margin = 0.1f
//        val requestedRatio = 4f / 3f
//        val midX = (correctedLocation.left + correctedLocation.right) / 2f
//        val midY = (correctedLocation.top + correctedLocation.bottom) / 2f
//        return if (view_finder.width < view_finder.height) {
//            RectF(
//                    midX - (1f + margin) * requestedRatio * correctedLocation.width() / 2f,
//                    midY - (1f - margin) * correctedLocation.height() / 2f,
//                    midX + (1f + margin) * requestedRatio * correctedLocation.width() / 2f,
//                    midY + (1f - margin) * correctedLocation.height() / 2f
//            )
//        } else {
//            RectF(
//                    midX - (1f - margin) * correctedLocation.width() / 2f,
//                    midY - (1f + margin) * requestedRatio * correctedLocation.height() / 2f,
//                    midX + (1f - margin) * correctedLocation.width() / 2f,
//                    midY + (1f + margin) * requestedRatio * correctedLocation.height() / 2f
//            )
//        }
//    }

}