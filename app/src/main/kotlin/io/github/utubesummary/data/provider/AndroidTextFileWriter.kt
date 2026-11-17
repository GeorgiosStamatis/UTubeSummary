package io.github.utubesummary.data.provider

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.utubesummary.domain.provider.TextFileWriter
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidTextFileWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) : TextFileWriter {
    override suspend fun writeText(uriString: String, text: String) {
        withContext(Dispatchers.IO) {
            // "wt" truncates a file that already exists.
            val stream = context.contentResolver.openOutputStream(uriString.toUri(), "wt")
                ?: throw FileNotFoundException(uriString)
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
    }
}
