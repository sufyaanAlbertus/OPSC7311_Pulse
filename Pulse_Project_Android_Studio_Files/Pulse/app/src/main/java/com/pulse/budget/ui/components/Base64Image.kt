package com.pulse.budget.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.pulse.budget.util.ImageUtils

/** Shows a photo that was stored in Firestore as Base64 text (receipts and profile pictures). */
@Composable
fun Base64Image(
    base64: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap = remember(base64) { base64?.let { ImageUtils.base64ToImageBitmap(it) } }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
