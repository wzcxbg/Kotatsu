package com.davemorrissey.labs.subscaleview

import android.util.Log
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import okio.Buffer
import okio.FileHandle
import okio.FileMetadata
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
import java.io.File
import java.io.RandomAccessFile
import java.util.Date

public class RarFileSystem(
	private val rarFile: File,
) : FileSystem() {
	private val metaDataEntries = ArrayList<MetaDataEntry>()

	init {
		val randomAccessFile = RandomAccessFile(rarFile, "r")
		val inStream = RandomAccessFileInStream(randomAccessFile)
		val inArchive = SevenZip.openInArchive(null, inStream)

		for (i in 0 until inArchive.numberOfItems) {
			val isFolder = inArchive.getProperty(i, PropID.IS_FOLDER) as Boolean
			val filePath = inArchive.getProperty(i, PropID.PATH) as String
			val fileSize = inArchive.getProperty(i, PropID.SIZE) as Long
			val createAtMillis = inArchive.getProperty(i, PropID.CREATION_TIME) as Date?
			val lastModifiedAtMillis =
				inArchive.getProperty(i, PropID.LAST_MODIFICATION_TIME) as Date?
			val lastAccessAtMillis = inArchive.getProperty(i, PropID.LAST_ACCESS_TIME) as Date?
			val defaultTimeMillis =
				(createAtMillis ?: lastModifiedAtMillis ?: lastAccessAtMillis)?.time
			val fileMetadata = FileMetadata(
				!isFolder, isFolder, null, fileSize.takeIf { !isFolder },
				createAtMillis?.time ?: defaultTimeMillis,
				lastModifiedAtMillis?.time ?: defaultTimeMillis,
				lastAccessAtMillis?.time ?: defaultTimeMillis,
			)
			val newPath = canonicalize(filePath.toPath()).toString()
			metaDataEntries.add(MetaDataEntry(i, newPath, fileMetadata))
		}
		inArchive.close()
		inStream.close()
	}

	override fun canonicalize(path: Path): Path {
		val canonical = "/".toPath().resolve(
			path, normalize = true
		)
		return canonical
	}

	override fun metadataOrNull(path: Path): FileMetadata? {
		return metaDataEntries.firstOrNull {
			it.filePath == path.toString()
		}?.fileMeta
	}

	override fun list(dir: Path): List<Path> {
		return listOrNull(dir)!!
	}

	override fun listOrNull(dir: Path): List<Path>? {
		val isRootPath = dir.toString() == Path.DIRECTORY_SEPARATOR
		val startIndex = when (isRootPath) {
			true -> dir.toString().length
			false -> dir.toString().length + 1
		}
		return metaDataEntries.filter {
			it.filePath.indexOf(Path.DIRECTORY_SEPARATOR, startIndex) == -1
				&& it.filePath.startsWith(dir.toString())
				&& it.filePath != dir.toString()
		}.map { it.filePath.toPath() }
			.takeIf { it.isNotEmpty() }
	}

	override fun source(file: Path): Source {
		Log.e("TAG", "RarFileSystem: source: $file")
		val canonicalizePath = canonicalize(file)
		val fileIndex = metaDataEntries.first {
			it.filePath == canonicalizePath.toString()
		}.fileIndex

		val randomAccessFile = RandomAccessFile(rarFile, "r")
		val inStream = RandomAccessFileInStream(randomAccessFile)
		val inArchive = SevenZip.openInArchive(null, inStream)

		val buffer = Buffer()
		inArchive.extractSlow(fileIndex) { data ->
			buffer.write(data)
			data.size
		}
		inArchive.close()
		inStream.close()
		return buffer
	}

	override fun openReadOnly(file: Path): FileHandle {
		throw UnsupportedOperationException("not implemented yet!")
	}

	override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
		throw IOException("zip entries are not writable")
	}

	override fun sink(file: Path, mustCreate: Boolean): Sink {
		throw IOException("rar file systems are read-only")
	}

	override fun appendingSink(file: Path, mustExist: Boolean): Sink {
		throw IOException("rar file systems are read-only")
	}

	override fun createDirectory(dir: Path, mustCreate: Boolean) {
		throw IOException("rar file systems are read-only")
	}

	override fun atomicMove(source: Path, target: Path) {
		throw IOException("rar file systems are read-only")
	}

	override fun delete(path: Path, mustExist: Boolean) {
		throw IOException("rar file systems are read-only")
	}

	override fun createSymlink(source: Path, target: Path) {
		throw IOException("rar file systems are read-only")
	}

	public data class MetaDataEntry(
		val fileIndex: Int,
		val filePath: String,
		val fileMeta: FileMetadata
	)
}
