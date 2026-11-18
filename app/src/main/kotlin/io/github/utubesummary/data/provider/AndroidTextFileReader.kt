package io.github.utubesummary.data.provider

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.utubesummary.domain.provider.FileTooLargeException
import io.github.utubesummary.domain.provider.TextFileReader
import io.github.utubesummary.util.decodeTextBytes
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidTextFileReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : TextFileReader {
    override suspend fun readText(uriString: String, maxBytes: Long): String =
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openInputStream(uriString.toUri())
                ?: throw FileNotFoundException(uriString)
            stream.use {
                // Read one byte more than allowed to detect an oversized file.
                val bytes = it.readNBytes((maxBytes + 1).toInt())
                if (bytes.size > maxBytes) throw FileTooLargeException(maxBytes)
                decodeTextBytes(bytes)
            }
        }
}
