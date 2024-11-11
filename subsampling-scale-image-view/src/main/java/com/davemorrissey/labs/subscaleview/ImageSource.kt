package com.davemorrissey.labs.subscaleview

import android.content.ContentResolver.SCHEME_FILE
import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.annotation.ReturnThis
import com.davemorrissey.labs.subscaleview.internal.URI_PATH_ASSET
import com.davemorrissey.labs.subscaleview.internal.URI_SCHEME_FILE
import com.davemorrissey.labs.subscaleview.internal.URI_SCHEME_ZIP
import java.io.File
import android.graphics.Bitmap as AndroidBitmap
import android.net.Uri as AndroidUri

public sealed class ImageSource(
	public var isTilingEnabled: Boolean,
) {

	internal var region: Rect? = null
		set(value) {
			field = value
			if (value != null) {
				sWidth = value.width()
				sHeight = value.height()
			}
		}

	public var sWidth: Int = 0
		protected set
	public var sHeight: Int = 0
		protected set

	public class Resource(@DrawableRes public val resourceId: Int) : ImageSource(true)

	public class Uri(public val uri: AndroidUri) : ImageSource(true)

	public class Bitmap @JvmOverloads constructor(
		public val bitmap: AndroidBitmap,
		public val isCached: Boolean = false,
	) : ImageSource(false) {
		init {
			sWidth = bitmap.width
			sHeight = bitmap.height
		}
	}

	@ReturnThis
	public fun region(left: Int, top: Int, right: Int, bottom: Int): ImageSource {
		region = Rect(left, top, right, bottom)
		return this
	}

	@ReturnThis
	public fun region(rect: Rect): ImageSource {
		region = Rect(rect)
		return this
	}

	@Suppress("FunctionName")
	public companion object {

		@JvmStatic
		public fun Uri(uri: String): Uri {
			var uriString = uri
			if (!uriString.contains("://")) {
				if (uriString.startsWith("/")) {
					uriString = uriString.substring(1)
				}
				uriString = "$SCHEME_FILE:///$uriString"
			}
			return Uri(AndroidUri.parse(uriString))
		}

		@JvmStatic
		public fun Asset(assetName: String): Uri = Uri(
			AndroidUri.fromParts(URI_SCHEME_FILE, URI_PATH_ASSET + assetName, null),
		)

		@JvmStatic
		public fun File(file: File): Uri = Uri(AndroidUri.fromFile(file))

		@JvmStatic
		public fun Zip(file: File, entry: String): Uri = Uri(
			AndroidUri.fromParts(URI_SCHEME_ZIP, file.absolutePath, entry),
		)
	}
}
