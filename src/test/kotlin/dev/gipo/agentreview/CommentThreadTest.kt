package dev.gipo.agentreview

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import dev.gipo.agentreview.diff.CommentInlayPanel
import dev.gipo.agentreview.model.Author
import dev.gipo.agentreview.model.Comment
import dev.gipo.agentreview.model.ThreadEntry
import java.text.DateFormat
import java.util.Date
import javax.swing.JComponent

class CommentThreadTest : BasePlatformTestCase() {
    private val comment = Comment(
        path = "a.kt",
        startLine = 1,
        text = "root",
        thread = listOf(ThreadEntry(Author.AGENT, "first"), ThreadEntry(Author.USER, "second")),
        reply = "done",
    )

    private fun labels(panel: JComponent): List<JBLabel> = UIUtil.findComponentsOfType(panel, JBLabel::class.java)

    fun testEachReplyIsItsOwnRowUnderItsAuthor() {
        val entries = listOf("root", "first", "second", "done")
        val sequence = labels(CommentInlayPanel(project, comment) {}).mapNotNull { label ->
            if (label.text == "You" || label.text == "Agent") label.text else entries.firstOrNull { ">$it<" in label.text }
        }
        assertEquals(listOf("You", "root", "Agent", "first", "You", "second", "Agent", "done"), sequence)
    }

    fun testLegacyReplyShowsNoTime() {
        val at = 1_700_000_000_000L
        val timed = comment.copy(createdAt = at, thread = comment.thread.map { it.copy(createdAt = at) })
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(at))
        assertEquals(3, labels(CommentInlayPanel(project, timed) {}).count { it.text == time })
    }

    fun testReplyTextIsNeitherItalicNorGrey() {
        val reply = labels(CommentInlayPanel(project, comment) {}).first { "first" in it.text }
        assertFalse(reply.text, "<i>" in reply.text)
        assertFalse(reply.foreground == UIUtil.getContextHelpForeground())
    }
}
