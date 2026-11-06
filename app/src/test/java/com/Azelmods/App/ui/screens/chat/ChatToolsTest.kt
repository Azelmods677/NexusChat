package com.Azelmods.App.ui.screens.chat

import com.Azelmods.App.data.chat.ChatFolder
import com.Azelmods.App.data.chat.ChatOrganizer
import com.Azelmods.App.ui.screens.group.GroupLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatToolsTest {
    @Test
    fun `una vista previa se guarda y se quita del texto`() {
        val preview = LinkPreview("https://example.com/a", "Título", "Descripción")
        val content = appendLinkPreview("mira https://example.com/a", preview)
        assertEquals("mira https://example.com/a", withoutLinkPreview(content))
        assertEquals(preview, decodeLinkPreview(content))
    }

    @Test
    fun `un texto normal no tiene vista previa`() {
        assertNull(decodeLinkPreview("hola"))
        assertEquals("hola", withoutLinkPreview("hola"))
    }

    @Test
    fun `solo se abren enlaces https y sin la puntuacion final`() {
        assertEquals("https://example.com/a", firstUrl("mira https://example.com/a."))
        assertNull(firstUrl("mira http://example.com/a"))
        assertNull(firstUrl("sin enlaces"))
    }

    @Test
    fun `el titulo sale de open graph antes que de la etiqueta title`() {
        val html = """<html><head><title>Otro</title>
            <meta property="og:title" content="Noticias &amp; más">
            <meta content="Resumen" name="description"></head></html>"""
        val preview = parseLinkPreview("https://example.com", html)
        assertEquals("Noticias & más", preview?.title)
        assertEquals("Resumen", preview?.description)
    }

    @Test
    fun `sin titulo no hay vista previa`() {
        assertNull(parseLinkPreview("https://example.com", "<html><body>nada</body></html>"))
    }

    @Test
    fun `un contacto se codifica y se recupera`() {
        val contact = SharedContact("uid1", "Lucía", null)
        assertEquals(contact, decodeContact(encodeContact(contact)))
        assertNull(decodeContact("hola"))
    }

    @Test
    fun `el enlace de grupo acepta enlace o codigo`() {
        assertEquals("ABC234", GroupLinks.parseCode(" nexuschat://join/abc234 "))
        assertEquals("ABC234", GroupLinks.parseCode("abc234"))
        assertTrue(GroupLinks.newCode().length == 12)
    }

    @Test
    fun `una mencion a medias se detecta y se completa`() {
        assertEquals("luc", currentMentionQuery("hola @luc"))
        assertEquals("", currentMentionQuery("@"))
        assertNull(currentMentionQuery("correo@ejemplo.com"))
        assertNull(currentMentionQuery("sin mencion"))
        assertEquals("hola @Lucía ", applyMention("hola @luc", "Lucía"))
    }

    @Test
    fun `solo se colorean las menciones`() {
        val styled = highlightMentions("hola @Lucía y correo@ejemplo.com", androidx.compose.ui.graphics.Color.Red)
        assertEquals(1, styled.spanStyles.size)
        assertEquals("@Lucía", styled.text.substring(styled.spanStyles[0].start, styled.spanStyles[0].end))
    }

    @Test
    fun `las carpetas sobreviven a guardarse`() {
        val folders = listOf(ChatFolder("1", "Trabajo", setOf("a", "b")), ChatFolder("2", "Vacía", emptySet()))
        assertEquals(folders, ChatOrganizer.parseFolders(ChatOrganizer.encodeFolders(folders)))
        assertEquals(emptyList<ChatFolder>(), ChatOrganizer.parseFolders(null))
    }
}
