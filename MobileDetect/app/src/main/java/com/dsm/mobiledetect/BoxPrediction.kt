package com.dsm.mobiledetect

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*
import android.graphics.RectF
import kotlin.properties.Delegates

class BoxPrediction constructor(context: Context?, attributeSet: AttributeSet?) :
    View(context, attributeSet) {

    // private val resultRectF: MutableList<RectF> = mutableListOf()
    var results:Array<Box> = emptyArray()
    var imageWidth by Delegates.notNull<Int>()
    var imageHeight by Delegates.notNull<Int>()
    private var boxPaint = Paint().apply {
        alpha = 200
        style = Paint.Style.STROKE
        strokeWidth = 10f
        textSize = 80f
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
            boxPaint.style = Paint.Style.STROKE
             canvas.drawRect(box.rect, boxPaint)
        }
    }

    fun drawPredictionBox(predictions: Array<Box>, width:Int, height:Int) {
        // val resultRectF = predictionsToRectF(predictions)
        this.results = predictions
        this.imageWidth = width
        this.imageHeight = height
        // this.resultRectF.clear()
        // this.resultRectF.addAll(resultRectF)
        invalidate()
    }


    private fun mapOutputCoordinates(location: RectF): RectF {
        val previewLocation = RectF(
                location.left,
                location.top,
                location.right,
                location.bottom
        )

        return previewLocation
    }

}