package io.github.utubesummary.data.provider

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.utubesummary.domain.provider.DocumentMetadataProvider
import io.github.utubesummary.llm.tools.getFileName
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidDocumentMetadataProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : DocumentMetadataProvider {
    override suspend fun getFileName(uriString: String): String {
        return getFileName(context, uriString.toUri())
    }
}
