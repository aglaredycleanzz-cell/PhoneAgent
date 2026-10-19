package io.phoneagent.mobile.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalCompanionTest {
    @Test
    fun recognizesArabicActionRequest() {
        assertTrue(LocalCompanion.isActionRequest("افتح الإعدادات"))
    }

    @Test
    fun recognizesEnglishActionRequest() {
        assertTrue(LocalCompanion.isActionRequest("build the project"))
    }

    @Test
    fun ordinaryDiscussionDoesNotBecomeTaskAutomatically() {
        assertFalse(LocalCompanion.isActionRequest("ما رأيك في هذه الفكرة؟"))
    }

    @Test
    fun savedActionReplyStatesThatItWasNotExecuted() {
        val reply = LocalCompanion.reply("nova", "افتح الإعدادات", taskWasSaved = true)
        assertTrue(reply.contains("مسودة"))
        assertTrue(reply.contains("لم أُنفّذه"))
    }

    @Test
    fun capabilityReplyIsHonestAboutMissingModel() {
        val reply = LocalCompanion.reply("nova", "ماذا تستطيع أن تفعل؟", taskWasSaved = false)
        assertTrue(reply.contains("ليست صادرة عن نموذج"))
    }

    @Test
    fun profilesHaveDifferentVoices() {
        assertFalse(
            LocalCompanion.reply("orion", "ساعدني في التخطيط", false) ==
                LocalCompanion.reply("sentinel", "ساعدني في التخطيط", false)
        )
    }

    @Test
    fun messageNormalizationCapsInput() {
        assertEquals(MAX_CHAT_MESSAGE_LENGTH, normalizeChatText("x".repeat(2500)).length)
    }
}
