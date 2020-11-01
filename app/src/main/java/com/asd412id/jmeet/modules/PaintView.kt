package com.asd412id.jmeet.modules

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Base64
import android.util.LayoutDirection
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.graphics.createBitmap
import com.asd412id.jmeet.R
import java.io.ByteArrayOutputStream

@SuppressLint("ResourceType")
class PaintView(context: Context?, attrs:AttributeSet) : View(context,attrs) {
    private val path = Path()
    private val brush = Paint()

    init{
        brush.isAntiAlias
        brush.color = Color.BLACK
        brush.style = Paint.Style.STROKE
        brush.strokeJoin = Paint.Join.ROUND
        brush.strokeWidth = 8f
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val pointX = event?.x
        val pointY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> path.moveTo(pointX!!,pointY!!)
            MotionEvent.ACTION_MOVE -> path.lineTo(pointX!!,pointY!!)
        }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path,brush)
    }

    fun clear(){
        path.reset()
        invalidate()
    }

    fun getBase64(): String{
        val baos = ByteArrayOutputStream()
        val bitmap = Bitmap.createBitmap(this.measuredWidth,this.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.draw(canvas)
        bitmap.compress(Bitmap.CompressFormat.PNG, 30, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }
}