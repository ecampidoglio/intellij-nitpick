package dev.gipo.agentreview

import com.intellij.mcpserver.McpExpectedError
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.runInEdtAndGet
import dev.gipo.agentreview.mcp.AgentReviewToolset
import java.io.File

class AgentNavigateTest : GitReviewTestCase() {
    private val toolset = AgentReviewToolset()

    fun testOpensTheDiffOfAFileInTheScopeAtTheLine() {
        reviewUncommittedEdit(edited = "a.txt", untouched = "b.txt")

        val result = timeoutRunBlocking { toolset.navigate(project, "a.txt", 3, "new") }

        assertTrue(result.in_scope)
        assertEquals(2, caretLineInDiffOf("a.txt"))
    }

    fun testOpensAProjectFileOutsideTheScopeAtTheLine() {
        reviewUncommittedEdit(edited = "a.txt", untouched = "b.txt")

        val result = timeoutRunBlocking { toolset.navigate(project, "b.txt", 2, "new") }

        assertFalse(result.in_scope)
        val (opened, caretLine) = runInEdtAndGet {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            editor?.virtualFile?.name to editor?.caretModel?.logicalPosition?.line
        }
        assertEquals("b.txt", opened)
        assertEquals(1, caretLine)
    }

    fun testDoesNotNotifyAboutAFileOutsideTheScope() {
        reviewUncommittedEdit(edited = "a.txt", untouched = "b.txt")
        val shown = mutableListOf<Notification>()
        project.messageBus.connect(testRootDisposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                shown += notification
            }
        })

        timeoutRunBlocking { toolset.navigate(project, "b.txt", 2, "new") }

        assertEmpty(shown.filter { it.groupId == "Nitpick" })
    }

    fun testFailsOnAFileThatDoesNotExist() {
        assertThrows(McpExpectedError::class.java) {
            timeoutRunBlocking { toolset.navigate(project, "no/such/file.kt", 1, "new") }
        }
    }

    fun testRefusesAFileOutsideTheProject() {
        val outside = File(File(project.basePath!!).parentFile, "nitpick-outside.txt").apply { writeText("line1\n") }
        try {
            VfsRootAccess.allowRootAccess(testRootDisposable, outside.path)
            VfsUtil.findFileByIoFile(outside, true)

            assertThrows(McpExpectedError::class.java) {
                timeoutRunBlocking { toolset.navigate(project, outside.path, 1, "new") }
            }
            assertEmpty(openFiles())
        } finally {
            outside.delete()
        }
    }
}
