package dev.gipo.agentreview

import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.gipo.agentreview.diff.CommentInlayPanel
import dev.gipo.agentreview.model.Author
import dev.gipo.agentreview.model.Comment
import dev.gipo.agentreview.model.ThreadEntry
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
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

    private fun layOut(c: Component) {
        if (c is Container) {
            c.doLayout()
            c.components.forEach(::layOut)
        }
    }

    private fun sideBar(width: Int, panel: CommentInlayPanel): JBScrollPane =
        JBScrollPane(ScrollablePanel(BorderLayout()).apply { add(panel, BorderLayout.NORTH) }).apply {
            setSize(width, 400)
            repeat(2) { layOut(this) }
        }

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

    fun testTextWrapsToTheSideBarWidth() {
        val long = comment.copy(text = "word ".repeat(200))
        val narrow = CommentInlayPanel(project, long) {}
        val pane = sideBar(300, narrow)
        val wide = CommentInlayPanel(project, long) {}
        sideBar(700, wide)
        labels(narrow).filter { it.text.startsWith("<html>") }.forEach { assertTrue(it.text, it.width <= pane.viewport.width) }
        assertTrue("${narrow.height} <= ${wide.height}", narrow.height > wide.height)
    }

    fun testTextKeepsItsWidthInTheEditor() {
        val text = labels(CommentInlayPanel(project, comment) {}).first { ">root<" in it.text }
        assertEquals(JBUI.scale(676) + text.insets.left + text.insets.right, text.preferredSize.width)
    }
}
