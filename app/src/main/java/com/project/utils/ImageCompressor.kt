package com.project.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File


object ImageCompressor {

    /**
     * Receives an image URI, center-crops it to a square, resizes to [maxDimension],
     * and returns the base64 encoded string.
     */
    fun compressAndEncodeToBase64(context: Context, uri: Uri, maxDimension: Int = 300): String? {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false
            
            // Decode bitmap with inSampleSize set
            val stream = contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(stream, null, options) ?: return null
            stream.close()
            
            // Center crop to a square
            bitmap = centerCrop(bitmap)
            
            // Scale exactly to maxDimension if needed
            if (bitmap.width > maxDimension) {
                bitmap = Bitmap.createScaledBitmap(bitmap, maxDimension, maxDimension, true)
            }
            
            // Compress to JPEG and encode
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
            val imageBytes = baos.toByteArray()
            
            return Base64.encodeToString(imageBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Decodes a raw Base64 string back into a ByteArray that Coil can render.
     */
    fun decodeBase64ToByteArray(base64Str: String): ByteArray? {
        if (base64Str.isBlank()) return null
        return try {
            Base64.decode(base64Str, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a temporary file in the cache directory and returns its FileProvider Uri.
     */
    fun createTempImageUri(context: Context): Uri {
        val cacheDir = File(context.cacheDir, "images")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val tempFile = File.createTempFile("camera_image_${System.currentTimeMillis()}", ".jpg", cacheDir)
        return FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            tempFile
        )
    }

    private fun centerCrop(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val minDim = minOf(width, height)
        
        val startX = (width - minDim) / 2
        val startY = (height - minDim) / 2
        
        return Bitmap.createBitmap(bitmap, startX, startY, minDim, minDim)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
