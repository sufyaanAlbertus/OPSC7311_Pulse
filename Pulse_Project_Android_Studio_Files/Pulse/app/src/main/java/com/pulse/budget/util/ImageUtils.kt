package com.pulse.budget.util

/*
 * References:
 *  - Android Developers (2025) Loading large bitmaps efficiently. https://developer.android.com/topic/performance/graphics/load-bitmap
 *  - Firebase (2025) Usage and limits (1 MiB document size). https://firebase.google.com/docs/firestore/quotas
 *  - Anthropic (2026) Claude [Large language model] — used as a coding assistant; see AI declaration in README.
 */

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

private const val TAG = "ImageUtils"

/** Compresses photos to Base64 so they can be stored in Firestore. */
object ImageUtils {

    /** Max width/height in pixels. */
    const val RECEIPT_MAX_DIMENSION = 1024
    const val PROFILE_MAX_DIMENSION = 256

    suspend fun uriToCompressedBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int,
        quality: Int = 60
    ): String? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver

            // 1. Read only the image size first, so a 12 MP photo isn't fully loaded into memory.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxDimension && bounds.outHeight / (sample * 2) >= maxDimension) {
                sample *= 2
            }

            // 2. Decode a down-sampled copy.
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@withContext null

            // 3. Rotate upright using the EXIF orientation (camera photos are often sideways).
            val rotation = resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            // 4. Scale so the longest side is at most maxDimension, applying the rotation too.
            val scale = minOf(1f, maxDimension / max(decoded.width, decoded.height).toFloat())
            val matrix = Matrix().apply {
                postScale(scale, scale)
                postRotate(rotation)
            }
            val finalBitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

            // 5. Compress to JPEG and encode as Base64 text for Firestore.
            val out = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            Log.d(TAG, "Compressed image: ${bounds.outWidth}x${bounds.outHeight} -> " +
                    "${finalBitmap.width}x${finalBitmap.height}, ${bytes.size / 1024} KB")
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Could not compress image $uri", e)
            null
        }
    }

    fun base64ToImageBitmap(base64: String): ImageBitmap? = try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        Log.e(TAG, "Could not decode Base64 image", e)
        null
    }
}
