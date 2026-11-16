package io.github.utubesummary.data

import androidx.room3.ColumnInfo
import androidx.room3.ColumnTypeConverters
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import io.github.utubesummary.data.converters.StringListConverter
import io.github.utubesummary.model.ProviderConfig
import io.github.utubesummary.util.SecurityUtil

@Entity(tableName = "ai_provider_config")
@ColumnTypeConverters(StringListConverter::class)
data class AIProviderConfigEntity(
    @PrimaryKey val provider: String,
    val apiKey: String,
    val baseUrl: String,
    @ColumnInfo(name = "active_model") val activeModel: String,
    val models: List<String>? = null,
) {
    fun toProviderConfig(): ProviderConfig {
        return ProviderConfig(
            apiKey = SecurityUtil.decrypt(apiKey),
            baseUrl = baseUrl,
            activeModel = activeModel,
            models = models ?: emptyList()
        )
    }

    companion object {
        fun fromProviderConfig(provider: String, config: ProviderConfig): AIProviderConfigEntity {
            return AIProviderConfigEntity(
                provider = provider,
                apiKey = SecurityUtil.encrypt(config.apiKey),
                baseUrl = config.baseUrl,
                activeModel = config.activeModel,
                models = config.models.takeIf { it.isNotEmpty() }
            )
        }
    }
}
