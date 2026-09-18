package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Utility for memory-safe bitmap operations and base64 encoding.
 * Prevents Out-Of-Memory (OOM) crashes by calculating appropriate inSampleSize.
 */
object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val DEFAULT_MAX_DIMENSION = 1280
    private const val JPEG_QUALITY = 80

    /**
     * Decodes a bitmap from a file with dimension bounding to prevent OOM.
     */
    fun decodeSampledBitmapFromFile(
        file: File,
        maxDimension: Int = DEFAULT_MAX_DIMENSION
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false

            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap from file: ${file.absolutePath}", e)
            null
        }
    }

    /**
     * Decodes a bitmap from an Android Uri (content:// or file://) safely with downsampling.
     */
    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = DEFAULT_MAX_DIMENSION
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap from uri: $uri", e)
            null
        }
    }

    /**
     * Compresses a bitmap to JPEG and encodes it into Base64 string for Gemini API payload.
     */
    fun bitmapToBase64Jpeg(bitmap: Bitmap, quality: Int = JPEG_QUALITY): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Calculates optimal downsampling factor (power of 2) for BitmapFactory.
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
