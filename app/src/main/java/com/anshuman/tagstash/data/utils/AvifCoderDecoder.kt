package com.anshuman.tagstash.data.utils

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.Dimension
import com.radzivon.bartoshyk.avif.coder.HeifCoder
import com.radzivon.bartoshyk.avif.coder.PreferredColorConfig
import com.radzivon.bartoshyk.avif.coder.ScaleMode
import com.radzivon.bartoshyk.avif.coder.ScalingQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AvifCoderDecoder(
    private val result: SourceResult,
    private val options: Options
) : Decoder {

    private val heifCoder = HeifCoder()

    override suspend fun decode(): DecodeResult? {
        val bytes = withContext(Dispatchers.IO) {
            result.source.source().readByteArray()
        }
        if (bytes.isEmpty() || (!heifCoder.isAvif(bytes) && !heifCoder.isHeif(bytes))) {
            return null
        }

        val targetWidth = when (val w = options.size.width) {
            is Dimension.Pixels -> w.px
            else -> 0
        }
        val targetHeight = when (val h = options.size.height) {
            is Dimension.Pixels -> h.px
            else -> 0
        }

        val bitmap: Bitmap = withContext(Dispatchers.IO) {
            if (targetWidth > 0 && targetHeight > 0) {
                heifCoder.decodeSampled(
                    byteArray = bytes,
                    scaledWidth = targetWidth,
                    scaledHeight = targetHeight,
                    preferredColorConfig = PreferredColorConfig.DEFAULT,
                    scaleMode = ScaleMode.FIT,
                    scaleQuality = ScalingQuality.DEFAULT
                )
            } else {
                heifCoder.decode(
                    byteArray = bytes,
                    preferredColorConfig = PreferredColorConfig.DEFAULT
                )
            }
        }

        return DecodeResult(
            drawable = BitmapDrawable(options.context.resources, bitmap),
            isSampled = targetWidth > 0 && targetHeight > 0
        )
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder? {
            val file = result.source.fileOrNull()
            val isAvif = result.mimeType == "image/avif" ||
                    (file != null && file.name.endsWith(".avif", ignoreCase = true))
            if (!isAvif) return null
            return AvifCoderDecoder(result, options)
        }
    }
}
