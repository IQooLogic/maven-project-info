package rs.devlabs.maven.project.info

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SimpleTextAttributes
import git4idea.branch.GitBranchUtil
import git4idea.repo.GitRepository

class SimpleGitBranchDecorator : ProjectViewNodeDecorator {
    override fun decorate(
        node: ProjectViewNode<*>?,
        presentation: PresentationData?
    ) {
        if (node == null || presentation == null) return
        if (node.name.isNullOrEmpty() || node.name.isNullOrBlank()) return

        val project = node.project ?: return
        val virtualFile = node.virtualFile ?: return

        if (!virtualFile.isDirectory) return
        if (!isMavenProject(virtualFile)) return

        val settings = service<SimpleProjectInfoSettings>()

        if (!settings.showGitBranch || !settings.enabled) return

        val branch = getBranchName(project, virtualFile)
        if (branch.isNullOrEmpty()) return

        presentation.addText(
            " $branch", SimpleTextAttributes(
            SimpleTextAttributes.STYLE_BOLD,
            if (settings.useColors) settings.getGitBranchColorRGB() else SimpleTextAttributes.GRAY_ATTRIBUTES.fgColor
        ))
    }

    private fun isMavenProject(virtualFile: VirtualFile): Boolean {
        return virtualFile.findChild("pom.xml") != null
    }

    private fun getBranchName(project: Project, virtualFile: VirtualFile): String? {
        // Get repository in background thread
        val repository = ReadAction.compute<GitRepository?, Throwable> {
            GitBranchUtil.guessWidgetRepository(project, virtualFile)
        }?: return ""

        // Get branch info
        return ReadAction.compute<String?, Throwable> {
            repository.currentBranch?.name ?: repository.currentRevision?.take(7)
        }
    }
}
