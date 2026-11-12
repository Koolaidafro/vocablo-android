package app.vocablo.tutor

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TutorTest {
    private val history = listOf(ChatMessage(Role.ASSISTANT, "¡Hola!"), ChatMessage(Role.USER, "Hola, yo soy bien"))

    @Test
    fun `claude requests use the messages endpoint, key header and low effort`() {
        val call = TutorApi.request(TutorConfig(Provider.CLAUDE, ""), "k", "sys", history)
        assertEquals("https://api.anthropic.com/v1/messages", call.url)
        assertEquals("k", call.headers["x-api-key"])
        assertEquals("2023-06-01", call.headers["anthropic-version"])
        val body = Json.parseToJsonElement(call.body).jsonObject
        assertEquals("claude-opus-5-5", body["model"]!!.jsonPrimitive.content)
        assertEquals("sys", body["system"]!!.jsonPrimitive.content)
        assertEquals("low", body["output_config"]!!.jsonObject["effort"]!!.jsonPrimitive.content)
        assertEquals("default", body["fallbacks"]!!.jsonPrimitive.content)
        assertEquals(listOf("assistant", "user"), body["messages"]!!.jsonArray.map { it.jsonObject["role"]!!.jsonPrimitive.content })
        assertFalse("temperature" in body)
    }

    @Test
    fun `other claude models skip the fallback beta`() {
        val call = TutorApi.request(TutorConfig(Provider.CLAUDE, "claude-haiku-5-5"), "k", "sys", history)
        assertFalse("anthropic-beta" in call.headers)
        assertFalse("fallbacks" in Json.parseToJsonElement(call.body).jsonObject)
    }

    @Test
    fun `openai style providers put the system prompt first and use bearer auth`() {
        val call = TutorApi.request(TutorConfig(Provider.GROK, ""), "xai-1", "sys", history)
        assertEquals("https://api.x.ai/v1/chat/completions", call.url)
        assertEquals("Bearer xai-1", call.headers["Authorization"])
        val messages = Json.parseToJsonElement(call.body).jsonObject["messages"]!!.jsonArray
        assertEquals("system", messages[0].jsonObject["role"]!!.jsonPrimitive.content)
        assertEquals(3, messages.size)
        val custom = TutorApi.request(TutorConfig(Provider.CUSTOM, "llama", "http://10.0.2.2:11434/v1/"), "x", "s", history)
        assertEquals("http://10.0.2.2:11434/v1/chat/completions", custom.url)
    }

    @Test
    fun `replies are read from each provider, skipping thinking blocks`() {
        val claude = """{"content":[{"type":"thinking","thinking":""},{"type":"text","text":"¡Muy bien!"}],"stop_reason":"end_turn"}"""
        assertEquals(Reply.Text("¡Muy bien!"), TutorApi.parse(Provider.CLAUDE, 200, claude))
        val openai = """{"choices":[{"message":{"role":"assistant","content":" Claro. "}}]}"""
        assertEquals(Reply.Text("Claro."), TutorApi.parse(Provider.OPENAI, 200, openai))
        val refusal = """{"content":[],"stop_reason":"refusal"}"""
        assertTrue(TutorApi.parse(Provider.CLAUDE, 200, refusal) is Reply.Failure)
    }

    @Test
    fun `errors become plain advice`() {
        val r = TutorApi.parse(Provider.OPENAI, 401, """{"error":{"message":"Incorrect API key"}}""")
        assertEquals(Reply.Failure("The key was refused. Check it in the tutor settings. (Incorrect API key)"), r)
        assertTrue((TutorApi.parse(Provider.CLAUDE, 529, "not json") as Reply.Failure).message.startsWith("The service is having trouble"))
    }

    @Test
    fun `the prompt follows the region`() {
        val spain = TutorPrompt.system(Scenarios.byId("cafe"), Variety.SPAIN, "beginner", listOf("el pan"), listOf("present"))
        assertTrue("vosotros for informal plural" in spain)
        assertTrue("el pan" in spain)
        val rio = TutorPrompt.system(Scenarios.byId("cafe"), Variety.RIOPLATENSE, "beginner", emptyList(), emptyList())
        assertTrue("voseo" in rio)
        assertEquals(Scenarios.all.first(), Scenarios.byId("nope"))
    }
}
