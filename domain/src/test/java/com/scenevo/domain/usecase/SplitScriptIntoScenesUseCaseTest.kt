package com.scenevo.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitScriptIntoScenesUseCaseTest {

    private val useCase = SplitScriptIntoScenesUseCase()

    @Test
    fun splitsBlankLineBlocks() {
        val script = """
            Kota hujan di malam hari.

            Cahaya neon memantul di aspal.
        """.trimIndent()

        val scenes = useCase(script)
        assertEquals(2, scenes.size)
        assertTrue(scenes[0].durationMs >= 1800L)
        assertEquals(0, scenes[0].index)
        assertEquals(1, scenes[1].index)
    }

    @Test
    fun emptyScriptReturnsEmpty() {
        assertTrue(useCase("   ").isEmpty())
    }
}
