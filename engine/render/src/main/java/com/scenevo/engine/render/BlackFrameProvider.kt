package com.scenevo.engine.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream

object BlackFrameProvider {

    private const val NAME = "scenevo_black_frame.png"

    fun getUri(context: Context): String {
        val file = File(context.cacheDir, NAME)
        if (!file.exists()) {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawColor(Color.BLACK)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
        }
        return file.absolutePath
    }
}
