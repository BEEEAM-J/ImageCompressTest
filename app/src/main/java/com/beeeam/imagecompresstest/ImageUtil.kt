package com.beeeam.imagecompresstest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtil {

    fun uriToOptimizeImageFile(context: Context, uri: Uri): File? {
        try {
            val storage = context.cacheDir
            val fileName = String.format("%s.%s", UUID.randomUUID(), "jpg")

            val tempFile = File(storage, fileName)
            tempFile.createNewFile()

//            val fos = FileOutputStream(tempFile)
//            decodeOptimizeBitmapFromUri(context, uri)?.apply {
//                compress(Bitmap.CompressFormat.JPEG, 100, fos)
//                recycle()
//            } ?: throw NullPointerException()

            decodeOptimizeBitmapFromUri(context, uri)?.apply {
                var compressQuality = 100
                do {
                    val fos = FileOutputStream(tempFile)
                    compress(Bitmap.CompressFormat.JPEG, compressQuality, fos)
                    compressQuality -= 5

                    fos.flush()
                    fos.close()
                } while (tempFile.length() / (1024.0 * 1024.0) > 2.0)
                recycle()
            } ?: throw NullPointerException()

            Log.d("Result", "압축된 파일 크기: ${tempFile.length()/ (1024.0 * 1024.0)}MB")

//            fos.flush()
//            fos.close()

            return tempFile
        } catch (e: Exception) {
            Log.d("ERROR", "${e.message}")
        }

        return null
    }

    private fun decodeOptimizeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        val input = BufferedInputStream(context.contentResolver.openInputStream(uri))

        input.mark(input.available())

        var bitmap: Bitmap?

        BitmapFactory.Options().run {
            inJustDecodeBounds = true
            bitmap = BitmapFactory.decodeStream(input, null, this)

            input.reset()

            inSampleSize = calculateInSampleSize(this)
            inJustDecodeBounds = false

            bitmap = rotateImageIfRequired(context, BitmapFactory.decodeStream(input, null, this)!!, uri)
        }

        input.close()

        return bitmap
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > 1440 || width > 1440) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= 1440 && halfWidth / inSampleSize >= 1440) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap? {
        val input = context.contentResolver.openInputStream(uri) ?: return null

        val exif =
            ExifInterface(input)

        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

        return rotateBitmap(bitmap, orientation)
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap? {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }

        return try {
            val bmRotated =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        }
    }
}
