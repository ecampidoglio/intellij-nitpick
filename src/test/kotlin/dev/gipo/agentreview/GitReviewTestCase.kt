package dev.gipo.agentreview

import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager
import com.intellij.openapi.vcs.changes.ui.ChangesTree
import com.intellij.openapi.vcs.changes.ui.VcsTreeModelData
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.replaceService
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.ui.UIUtil
import dev.gipo.agentreview.diff.EditorReviewBinding
import dev.gipo.agentreview.model.Side
import dev.gipo.agentreview.scope.ReviewChangesModel
import dev.gipo.agentreview.ui.ReviewToolWindowPanel
import git4idea.GitVcs
import kotlinx.coroutines.delay
import java.io.File

abstract class GitReviewTestCase : HeavyPlatformTestCase() {

    override fun runInDispatchThread(): Boolean = false

    override fun setUp() {
        super.setUp()
        val scope = (project as ComponentManagerEx).getCoroutineScope().childScope(javaClass.simpleName)
        project.replaceService(FileEditorManager::class.java, FileEditorManagerImpl(project, scope), testRootDisposable)
    }

    override fun tearDown() {
        try {
            runInEdtAndWait { FileEditorManagerEx.getInstanceEx(project).closeAllFiles() }
        } finally {
            super.tearDown()
        }
    }

    protected fun reviewUncommittedEdit(edited: String, untouched: String) {
        val root = getOrCreateProjectBaseDir()
        val dir = File(root.path)
        val lines = "line1\nline2\nline3\nline4\nline5\n"
        File(dir, edited).writeText(lines)
        File(dir, untouched).writeText(lines)
        git(dir, "init")
        git(dir, "add", edited, untouched)
        git(dir, "commit", "-m", "init")
        File(dir, edited).writeText("changed\n" + lines.substringAfter("\n"))
        VfsUtil.markDirtyAndRefresh(false, true, true, root)
        ProjectLevelVcsManager.getInstance(project).setDirectoryMappings(listOf(VcsDirectoryMapping(root.path, GitVcs.NAME)))
        VcsDirtyScopeManager.getInstance(project).markEverythingDirty()
        ChangeListManagerImpl.getInstanceImpl(project).waitUntilRefreshed()
        ReviewChangesModel.getInstance(project).refresh()
    }

    private fun git(dir: File, vararg args: String) {
        val builder = ProcessBuilder("git", "-c", "user.name=test", "-c", "user.email=test@example.com", *args)
            .directory(dir).redirectErrorStream(true)
        builder.environment()["GIT_CONFIG_GLOBAL"] = "/dev/null"
        builder.environment()["GIT_CONFIG_NOSYSTEM"] = "1"
        val process = builder.start()
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(output, 0, process.waitFor())
    }

    protected fun openToolWindowListingTheChanges() {
        val panel = runInEdtAndGet { ReviewToolWindowPanel(project, testRootDisposable) }
        timeoutRunBlocking {
            while (!runInEdtAndGet { VcsTreeModelData.all(UIUtil.findComponentOfType(panel, ChangesTree::class.java)!!).iterateUserObjects(Change::class.java).isNotEmpty }) {
                delay(50)
            }
        }
    }

    protected fun openFiles(): Array<VirtualFile> = runInEdtAndGet { FileEditorManager.getInstance(project).openFiles }

    protected fun caretLineInDiffOf(path: String): Int = timeoutRunBlocking {
        var binding = newSideBinding(path)
        while (binding == null) {
            delay(50)
            binding = newSideBinding(path)
        }
        runInEdtAndGet { binding.editor.caretModel.logicalPosition.line }
    }

    private fun newSideBinding(path: String): EditorReviewBinding? = runInEdtAndGet {
        EditorFactory.getInstance().allEditors
            .mapNotNull { it.getUserData(EditorReviewBinding.KEY) }
            .firstOrNull { it.primarySide == Side.NEW && it.path == path }
    }
}
