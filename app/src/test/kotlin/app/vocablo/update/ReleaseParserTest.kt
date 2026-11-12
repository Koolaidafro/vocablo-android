package app.vocablo.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParserTest {
    private val body = """
        {"tag_name":"v1.0.123","body":"Faster Verb Rain\n","assets":[
          {"name":"notes.txt","browser_download_url":"https://example.com/notes.txt"},
          {"name":"vocablo.apk","browser_download_url":"https://github.com/x/releases/download/v1.0.123/vocablo.apk"}]}
    """.trimIndent()

    @Test
    fun readsTheVersionAndApk() {
        val r = ReleaseParser.parse(body)!!
        assertEquals(123, r.versionCode)
        assertEquals("1.0.123", r.versionName)
        assertEquals("https://github.com/x/releases/download/v1.0.123/vocablo.apk", r.apkUrl)
        assertEquals("Faster Verb Rain", r.notes)
    }

    @Test
    fun onlyNewerBuildsAreOffered() {
        val r = ReleaseParser.parse(body)!!
        assertTrue(ReleaseParser.isNewer(r, 122))
        assertFalse(ReleaseParser.isNewer(r, 123))
    }

    @Test
    fun releasesWithoutAnApkAreIgnored() {
        assertNull(ReleaseParser.parse("""{"tag_name":"v1.0.5","assets":[]}"""))
        assertNull(ReleaseParser.parse("not json"))
    }
}
