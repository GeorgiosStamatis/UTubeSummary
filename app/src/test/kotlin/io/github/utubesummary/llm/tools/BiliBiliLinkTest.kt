package io.github.utubesummary.llm.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BiliBiliLinkTest {

    private val bvid = "BV1xx411c7mD"

    @Test
    fun `a link without a scheme gets https and others are kept`() {
        assertEquals("https://www.bilibili.com/video/$bvid", ensureHttpsScheme("www.bilibili.com/video/$bvid"))
        assertEquals("http://b23.tv/abc", ensureHttpsScheme("http://b23.tv/abc"))
        assertEquals("https://b23.tv/abc", ensureHttpsScheme("https://b23.tv/abc"))
    }

    @Test
    fun `protocol relative subtitle links get https`() {
        assertEquals("https://i0.hdslb.com/s.json", protocolRelativeToHttps("//i0.hdslb.com/s.json"))
        assertEquals("https://i0.hdslb.com/s.json", protocolRelativeToHttps("https://i0.hdslb.com/s.json"))
        assertEquals("", protocolRelativeToHttps(""))
    }

    @Test
    fun `the id after video is found`() {
        assertEquals(bvid, bvidFromPathSegments(listOf("video", bvid)))
        assertEquals(bvid, bvidFromPathSegments(listOf("Video", bvid, "extra")))
    }

    @Test
    fun `an id as the first segment is found after a redirect`() {
        assertEquals(bvid, bvidFromPathSegments(listOf(bvid)))
    }

    @Test
    fun `anything that is not a video id gives null`() {
        assertNull(bvidFromPathSegments(emptyList()))
        assertNull(bvidFromPathSegments(listOf("video", "not-an-id")))
        assertNull(bvidFromPathSegments(listOf("bangumi", bvid)))
        assertNull(bvidFromPathSegments(listOf("BV123")))
    }
}
