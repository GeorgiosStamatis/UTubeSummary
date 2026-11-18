package io.github.utubesummary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Keeps the layers apart by reading the imports of the source files:
 * `ui` and `vm` talk to use cases only, `domain` and `model` know nothing about Android UI, storage
 * or dependency injection, and `model` is plain data.
 */
class ArchitectureRulesTest {

    private val base = "io.github.utubesummary"

    private val sourceRoot: File? = listOf(
        File("src/main/kotlin/io/github/utubesummary"),
        File("app/src/main/kotlin/io/github/utubesummary"),
    ).firstOrNull { it.isDirectory }

    private val importPattern = Regex("^import\\s+([\\w.]+)", RegexOption.MULTILINE)

    /** "File.kt imports x.y.Z" for every import in [folders] that starts with a forbidden prefix. */
    private fun violations(
        folders: List<String>,
        forbidden: List<String>,
        allowed: Set<String> = emptySet(),
    ): List<String> {
        val root = sourceRoot
        assumeTrue(root != null, "source folder not found from ${File(".").absolutePath}")
        return folders.flatMap { folder ->
            File(root, folder).walkTopDown().filter { it.isFile && it.extension == "kt" }.flatMap { file ->
                importPattern.findAll(file.readText()).map { it.groupValues[1] }
                    .filter { import -> forbidden.any { import.startsWith(it) } && import !in allowed }
                    .map { import -> "${file.name} imports $import" }
            }.toList()
        }
    }

    private fun imports(vararg prefixes: String) = prefixes.map { "$base.$it" }

    @Test
    fun `model is plain data and imports no other layer`() {
        val found = violations(
            folders = listOf("model"),
            forbidden = imports("ui.", "vm.", "data.", "di.", "domain.", "llm.") + listOf("android.", "androidx.compose"),
        )

        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun `domain and exceptions do not know the ui, view models, storage or injection modules`() {
        val found = violations(
            folders = listOf("domain", "exception"),
            forbidden = imports("ui.", "vm.", "data.", "di.") + listOf("androidx.compose"),
        )

        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun `use cases do not use the Android framework`() {
        val found = violations(
            folders = listOf("domain/usecase"),
            forbidden = listOf("android."),
        )

        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun `view models use use cases only`() {
        val found = violations(
            folders = listOf("vm"),
            forbidden = imports("data.", "domain.repository.", "di.", "ui."),
        )

        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun `the ui does not reach into storage, repositories or injection modules`() {
        val found = violations(
            folders = listOf("ui"),
            forbidden = imports("data.", "domain.repository.", "di."),
        )

        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun `the ui uses the llm package only for the few shared helpers`() {
        val allowed = setOf(
            "AIProvider",
            "defaultSystemPromptPlaceholder",
            "generateFinalPromptString",
            "tools.getFileName",
            // Moves to the model layer in the same batch; remove these five when it has.
            "CRITICAL_ANALYST_PROFILE_ID",
            "CURIOUS_LEARNER_PROFILE_ID",
            "QUICK_BRIEF_PROFILE_ID",
            "TECHNICAL_ANALYSIS_PROFILE_ID",
            "TECHNICAL_CONTENT_EVALUATOR_PROFILE_ID",
        ).map { "$base.llm.$it" }.toSet()

        val found = violations(
            folders = listOf("ui"),
            forbidden = imports("llm."),
            allowed = allowed,
        )

        assertEquals(emptyList<String>(), found)
    }
}
