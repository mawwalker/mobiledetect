package com.dsm.mobiledetect

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import java.util.*

class BoxPrediction constructor(context: Context?, attributeSet: AttributeSet?) :
    View(context, attributeSet) {

    private val resultRectF: MutableList<RectF> = mutableListOf()
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
            boxPaint.style = Paint.Style.STROKE
            canvas.drawRect(box.rect, boxPaint)
        }
        // resultRectF.forEach { canvas.drawRect(it, boxPaint) }
    }

    fun drawPredictionBox(predictions: Array<Box>) {
        val resultRectF = predictionsToRectF(predictions)
        this.results = predictions
        this.resultRectF.clear()
        this.resultRectF.addAll(resultRectF)
        invalidate()
    }
    private fun predictionsToRectF(predictions:Array<Box>):MutableList<RectF>{
        val resultRectF:MutableList<RectF> = mutableListOf()
        for (box in predictions){
            resultRectF.add(box.rect)
        }
        return resultRectF
    }
}