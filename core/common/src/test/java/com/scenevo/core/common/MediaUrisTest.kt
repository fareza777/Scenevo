package com.scenevo.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MediaUrisTest {
    @Test
    fun parsesAbsolutePathAsFileUri() {
        val uri = MediaUris.parse("/data/user/0/com.scenevo/files/tts/voice.wav")
        assertEquals("file", uri.scheme)
        assertTrue(uri.path!!.endsWith("voice.wav"))
    }

    @Test
    fun keepsContentUri() {
        val uri = MediaUris.parse("content://media/external/images/1")
        assertEquals("content", uri.scheme)
    }

    @Test
    fun fileUriRoundTrip() {
        val file = File("/tmp/sample.wav")
        val encoded = MediaUris.fileUri(file)
        assertTrue(encoded.startsWith("file:"))
        assertEquals("file", MediaUris.parse(encoded).scheme)
    }
}
