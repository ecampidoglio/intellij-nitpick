package dev.gipo.agentreview

import com.intellij.testFramework.runInEdtAndWait
import dev.gipo.agentreview.scope.ReviewChangesModel

class DiffNavigationTest : GitReviewTestCase() {

    fun testMovesWithinADiffThatThePreviewAlreadyShows() {
        reviewUncommittedEdit(edited = "a.txt", untouched = "b.txt")
        openToolWindowListingTheChanges()
        navigate("a.txt", 3)
        assertEquals(2, caretLineInDiffOf("a.txt"))

        navigate("a.txt", 5)

        assertEquals(4, caretLineInDiffOf("a.txt"))
        assertSize(1, openFiles())
    }

    fun testKeepsOneDiffTabWithoutTheToolWindow() {
        reviewUncommittedEdit(edited = "a.txt", untouched = "b.txt")
        navigate("a.txt", 3)
        assertEquals(2, caretLineInDiffOf("a.txt"))

        navigate("a.txt", 5)

        assertSize(1, openFiles())
        assertEquals(4, caretLineInDiffOf("a.txt"))
    }

    private fun navigate(path: String, line: Int) = runInEdtAndWait {
        ReviewChangesModel.getInstance(project).navigate(path, line)
    }
}
