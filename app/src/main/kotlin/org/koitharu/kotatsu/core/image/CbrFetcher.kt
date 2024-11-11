package org.koitharu.kotatsu.core.image

import android.net.Uri
import android.webkit.MimeTypeMap
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.toAndroidUri
import kotlinx.coroutines.runInterruptible
import okio.Path.Companion.toPath
import org.koitharu.kotatsu.core.util.ext.isRarUri
import com.davemorrissey.labs.subscaleview.RarFileSystem
import java.io.File
import coil3.Uri as CoilUri

class CbrFetcher(
	private val uri: Uri,
	private val options: Options,
) : Fetcher {

	override suspend fun fetch() = runInterruptible {
		val filePath = uri.schemeSpecificPart.toPath()
		val entryName = requireNotNull(uri.fragment)
		SourceFetchResult(
			source = ImageSource(entryName.toPath(), RarFileSystem(File(filePath.toString()))),
			mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(entryName.substringAfterLast('.', "")),
			dataSource = DataSource.DISK,
		)
	}

	class Factory : Fetcher.Factory<CoilUri> {

		override fun create(
			data: CoilUri,
			options: Options,
			imageLoader: ImageLoader
		): Fetcher? {
			val androidUri = data.toAndroidUri()
			return if (androidUri.isRarUri()) {
				CbrFetcher(androidUri, options)
			} else {
				null
			}
		}
	}
}
