package dev.gipo.agentreview

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.vcs.FileStatus
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.gipo.agentreview.diff.CommentColors
import dev.gipo.agentreview.diff.CommentInlayPanel
import dev.gipo.agentreview.model.Comment
import dev.gipo.agentreview.model.CommentType
import java.awt.Color
import java.awt.image.BufferedImage

class CommentCardThemeTest : BasePlatformTestCase() {
    private val scheme get() = EditorColorsManager.getInstance().globalScheme
    private val undo = mutableListOf<() -> Unit>()

    override fun tearDown() {
        try {
            undo.forEach { it() }
        } finally {
            super.tearDown()
        }
    }

    private fun attributes(key: TextAttributesKey, change: TextAttributes.() -> Unit) {
        val original = scheme.getAttributes(key)
        undo += { scheme.setAttributes(key, original) }
        scheme.setAttributes(key, original.clone().apply(change))
    }

    private fun color(key: ColorKey, color: Color) {
        val original = scheme.getColor(key)
        undo += { scheme.setColor(key, original) }
        scheme.setColor(key, color)
    }

    fun testCardBackgroundFollowsTheEditorColorScheme() {
        attributes(HighlighterColors.TEXT) { backgroundColor = Color(0x2E3440) }
        val panel = CommentInlayPanel(project, Comment(path = "a.kt", startLine = 1, text = "hey")) {}
        panel.setSize(600, 100)
        val image = BufferedImage(600, 100, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        try {
            panel.paint(g)
        } finally {
            g.dispose()
        }
        assertEquals(Color(0x2E3440), Color(image.getRGB(300, panel.insets.top + 3)))
    }

    fun testIssueAccentIsTheErrorUnderlineColor() {
        attributes(CodeInsightColors.ERRORS_ATTRIBUTES) {
            effectColor = Color(0xBF616A)
            errorStripeColor = Color(0x9E2927)
        }
        assertEquals(Color(0xBF616A).rgb, CommentColors.of(CommentType.ISSUE).rgb)
    }

    fun testNoteAccentIsTheWarningUnderlineColor() {
        attributes(CodeInsightColors.WARNINGS_ATTRIBUTES) {
            effectColor = Color(0xEBCB8B)
            errorStripeColor = Color(0xBE9117)
        }
        assertEquals(Color(0xEBCB8B).rgb, CommentColors.of(CommentType.NOTE).rgb)
    }

    fun testNoteAccentIsTheWarningStripeColorWithoutAnUnderline() {
        attributes(CodeInsightColors.WARNINGS_ATTRIBUTES) {
            effectColor = null
            errorStripeColor = Color(0xEBC700)
        }
        assertEquals(Color(0xEBC700).rgb, CommentColors.of(CommentType.NOTE).rgb)
    }

    fun testQuestionAccentIsTheLinkColor() {
        attributes(EditorColors.REFERENCE_HYPERLINK_COLOR) { foregroundColor = Color(0x88C0D0) }
        assertEquals(Color(0x88C0D0).rgb, CommentColors.of(CommentType.QUESTION).rgb)
    }

    fun testNitAccentIsTheLineCommentColor() {
        attributes(DefaultLanguageHighlighterColors.LINE_COMMENT) { foregroundColor = Color(0x616E88) }
        assertEquals(Color(0x616E88).rgb, CommentColors.of(CommentType.NIT).rgb)
    }

    fun testPraiseAndResolvedAccentsAreTheAddedFileColor() {
        color(FileStatus.ADDED.colorKey, Color(0xA3BE8C))
        assertEquals(Color(0xA3BE8C).rgb, CommentColors.of(CommentType.PRAISE).rgb)
        assertEquals(Color(0xA3BE8C).rgb, CommentColors.resolved.rgb)
    }

    fun testCommentedLineIsTintedWithTheNoteAccent() {
        attributes(HighlighterColors.TEXT) { backgroundColor = Color(0x2E3440) }
        attributes(CodeInsightColors.WARNINGS_ATTRIBUTES) { effectColor = Color(0xEBCB8B) }
        assertEquals(Color(0x54524F).rgb, CommentColors.lineBackground(resolved = false).rgb)
    }

    fun testResolvedLineIsTintedWithTheResolvedAccent() {
        attributes(HighlighterColors.TEXT) { backgroundColor = Color(0x2E3440) }
        color(FileStatus.ADDED.colorKey, Color(0xA3BE8C))
        assertEquals(Color(0x45504F).rgb, CommentColors.lineBackground(resolved = true).rgb)
    }
}
