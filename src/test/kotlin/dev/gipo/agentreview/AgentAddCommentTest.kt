package dev.gipo.agentreview

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.PsiTestUtil
import dev.gipo.agentreview.mcp.AgentReviewToolset
import dev.gipo.agentreview.model.Scope
import dev.gipo.agentreview.model.ScopeKind
import dev.gipo.agentreview.scope.ReviewChangesModel
import dev.gipo.agentreview.store.ReviewStore

class AgentAddCommentTest : HeavyPlatformTestCase() {
    private val toolset = AgentReviewToolset()

    private fun reviewBranchWith(text: String): VirtualFile {
        val dir = getOrCreateProjectBaseDir()
        val file = createChildData(dir, "a.kt")
        setFileText(file, text)
        PsiTestUtil.addContentRoot(module, dir)
        ReviewStore.getInstance(project).setScope(Scope(ScopeKind.BRANCH, head = "main"))
        ReviewChangesModel.getInstance(project).refresh()
        return file
    }

    fun testLineCommentFollowsItsTextWhenTheFileChanges() {
        val file = reviewBranchWith("a\nb\nc\n")
        toolset.addComment(project, "a.kt", "why?", line = 2, endLine = 0, type = "question")

        setFileText(file, "x\na\nb\nc\n")

        val placed = ReviewChangesModel.getInstance(project).comments().single()
        assertFalse(placed.outdated)
        assertEquals(3, placed.startLine)
    }

    fun testRangeCommentFollowsItsTextWhenTheFileChanges() {
        val file = reviewBranchWith("a\nb\nc\n")
        toolset.addComment(project, "a.kt", "why?", line = 2, endLine = 3, type = "question")

        setFileText(file, "x\na\nb\nc\n")

        val placed = ReviewChangesModel.getInstance(project).comments().single()
        assertFalse(placed.outdated)
        assertEquals(3, placed.startLine)
        assertEquals(4, placed.endLine)
    }
}
