package dev.gipo.agentreview.diff

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.vcs.FileStatus
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import dev.gipo.agentreview.model.CommentType
import java.awt.Color

object CommentColors {
    fun of(type: CommentType): Color = when (type) {
        CommentType.ISSUE -> themed(JBColor(0xD32F2F, 0xEF5350)) { it.underline(CodeInsightColors.ERRORS_ATTRIBUTES) }
        CommentType.QUESTION -> themed(JBColor(0x1976D2, 0x64B5F6)) { it.getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR)?.foregroundColor }
        CommentType.NIT -> themed(JBColor(0x757575, 0x9E9E9E)) { it.getAttributes(DefaultLanguageHighlighterColors.LINE_COMMENT)?.foregroundColor }
        CommentType.PRAISE -> resolved
        CommentType.NOTE -> themed(JBColor(0xF9A825, 0xFFD54F)) { it.underline(CodeInsightColors.WARNINGS_ATTRIBUTES) }
    }

    val resolved: Color = themed(JBColor(0x2E7D32, 0x81C784)) { it.getColor(FileStatus.ADDED.colorKey) }

    val cardBackground: Color = JBColor.lazy { EditorColorsManager.getInstance().globalScheme.defaultBackground }

    fun lineBackground(resolved: Boolean): Color {
        val accent = if (resolved) this.resolved else of(CommentType.NOTE)
        return JBColor.lazy { ColorUtil.mix(cardBackground, accent, 0.2) }
    }

    private fun themed(fallback: Color, pick: (EditorColorsScheme) -> Color?): Color =
        JBColor.lazy { pick(EditorColorsManager.getInstance().globalScheme) ?: fallback }

    private fun EditorColorsScheme.underline(key: TextAttributesKey): Color? =
        getAttributes(key)?.let { it.effectColor ?: it.errorStripeColor }
}
