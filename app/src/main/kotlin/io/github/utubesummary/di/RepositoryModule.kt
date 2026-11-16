package io.github.utubesummary.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.utubesummary.data.provider.AndroidAppLocaleProvider
import io.github.utubesummary.data.provider.AndroidDocumentMetadataProvider
import io.github.utubesummary.data.repository.AIProviderConfigRepositoryImpl
import io.github.utubesummary.data.repository.HistoryRepositoryImpl
import io.github.utubesummary.data.repository.UserPreferencesRepositoryImpl
import io.github.utubesummary.domain.provider.AppLocaleProvider
import io.github.utubesummary.domain.provider.ProviderConnectionChecker
import io.github.utubesummary.data.provider.AndroidTextFileReader
import io.github.utubesummary.data.provider.InMemorySourceTextStore
import io.github.utubesummary.domain.provider.TextFileReader
import io.github.utubesummary.domain.usecase.ImportVideoSummariesUseCase
import io.github.utubesummary.domain.usecase.ImportVideoSummariesUseCaseImpl
import io.github.utubesummary.data.provider.KtorProviderConnectionChecker
import io.github.utubesummary.domain.provider.SourceTextStore
import io.github.utubesummary.domain.usecase.AskAboutContentUseCase
import io.github.utubesummary.domain.usecase.AskAboutContentUseCaseImpl
import io.github.utubesummary.domain.provider.DocumentMetadataProvider
import io.github.utubesummary.domain.repository.AIProviderConfigRepository
import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.domain.usecase.SummarizeContentUseCase
import io.github.utubesummary.domain.usecase.SummarizeContentUseCaseImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAIProviderConfigRepository(impl: AIProviderConfigRepositoryImpl): AIProviderConfigRepository

    @Binds
    @Singleton
    abstract fun bindDocumentMetadataProvider(impl: AndroidDocumentMetadataProvider): DocumentMetadataProvider

    @Binds
    @Singleton
    abstract fun bindAppLocaleProvider(impl: AndroidAppLocaleProvider): AppLocaleProvider

    @Binds
    @Singleton
    abstract fun bindProviderConnectionChecker(impl: KtorProviderConnectionChecker): ProviderConnectionChecker

    @Binds
    @Singleton
    abstract fun bindSourceTextStore(impl: InMemorySourceTextStore): SourceTextStore

    @Binds
    @Singleton
    abstract fun bindAskAboutContentUseCase(impl: AskAboutContentUseCaseImpl): AskAboutContentUseCase

    @Binds
    @Singleton
    abstract fun bindTextFileReader(impl: AndroidTextFileReader): TextFileReader

    @Binds
    @Singleton
    abstract fun bindImportVideoSummariesUseCase(impl: ImportVideoSummariesUseCaseImpl): ImportVideoSummariesUseCase

    @Binds
    @Singleton
    abstract fun bindSummarizeContentUseCase(impl: SummarizeContentUseCaseImpl): SummarizeContentUseCase
}
