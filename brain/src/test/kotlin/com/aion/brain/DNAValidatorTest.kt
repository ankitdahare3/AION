package com.aion.brain

import org.junit.Assert.assertTrue
import org.junit.Test

private val validTool =
    ToolSchema(name = "send_email", sideEffect = true, inputSchema = "{}", description = "Sends an email")

private val validManifest =
    PluginManifest(
        id = "com.aion.plugin.gmail",
        name = "Gmail",
        version = "1.2.0",
        apiLevel = DNAValidator.SUPPORTED_API_LEVEL,
        permissions = listOf("INTERNET", "READ_MAIL_SCOPE"),
        tools = listOf(validTool),
        dna = DnaConfig(learn = true, reflect = true, benchmark = "bench/gmail.yaml", update = true),
    )

class DNAValidatorTest {
    @Test
    fun `a well-formed manifest passes all 4 gates`() {
        assertTrue(DNAValidator.validate(validManifest) is ValidationResult.Valid)
    }

    @Test
    fun `gate 1 - a manifest with no tools fails`() {
        val result = DNAValidator.validate(validManifest.copy(tools = emptyList()))

        assertInvalidContaining(result, "at least one tool")
    }

    @Test
    fun `gate 1 - a tool missing a description fails`() {
        val result = DNAValidator.validate(validManifest.copy(tools = listOf(validTool.copy(description = ""))))

        assertInvalidContaining(result, "description")
    }

    @Test
    fun `gate 2 - an unsupported apiLevel fails`() {
        val result = DNAValidator.validate(validManifest.copy(apiLevel = 99))

        assertInvalidContaining(result, "apiLevel")
    }

    @Test
    fun `gate 3 - an unknown permission fails`() {
        val result = DNAValidator.validate(validManifest.copy(permissions = listOf("ROOT_ACCESS")))

        assertInvalidContaining(result, "ROOT_ACCESS")
    }

    @Test
    fun `gate 4 - claiming learn without a benchmark fails`() {
        val result = DNAValidator.validate(validManifest.copy(dna = DnaConfig(learn = true, benchmark = null)))

        assertInvalidContaining(result, "benchmark")
    }

    @Test
    fun `gate 4 - claiming reflect without a benchmark fails`() {
        val result = DNAValidator.validate(validManifest.copy(dna = DnaConfig(reflect = true, benchmark = null)))

        assertInvalidContaining(result, "benchmark")
    }

    @Test
    fun `no learn or reflect claim means no benchmark is required`() {
        val result =
            DNAValidator.validate(
                validManifest.copy(dna = DnaConfig(learn = false, reflect = false, benchmark = null)),
            )

        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `multiple gate failures are all reported, not just the first`() {
        val result = DNAValidator.validate(validManifest.copy(apiLevel = 99, permissions = listOf("ROOT_ACCESS")))

        require(result is ValidationResult.Invalid)
        assertTrue(result.reasons.size >= 2)
    }

    private fun assertInvalidContaining(
        result: ValidationResult,
        substring: String,
    ) {
        require(result is ValidationResult.Invalid) { "expected Invalid, got $result" }
        assertTrue(
            "expected a reason containing \"$substring\", got ${result.reasons}",
            result.reasons.any {
                it.contains(substring)
            },
        )
    }
}
