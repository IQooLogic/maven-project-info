package rs.devlabs.maveninfo

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SimpleTextAttributes
import git4idea.branch.GitBranchUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/*
    TODO: implement bold/italic settings
    TODO: implement pattern for display ($version | $branch) -> [$version $branch]
    TODO: implement settings for color when version contains SNAPSHOT
    TODO: if gradle project git branch and tags should work
    TODO: fix enabling checkboxes do not apply on clicking apply in settings
    TODO: remove tags - too slow
    TODO: make small plugin with only project version and git branch decorators (separate) -> 8.2.4-SNAPSHOT dev
    TODO: add settings to enable/disable all and one by one, project version and git branch with ability to change color for both including bold/italic/plain
    TODO: add settings to change icons for project info
 */
class ProjectInfoDecorator : ProjectViewNodeDecorator {
    private val logger = LoggerFactory.getLogger("ProjectInfoDecorator")
    private val gitInfoCache = ConcurrentHashMap<String, GitInfo>()

    data class GitInfo(
        val branch: String? = null,
        val tag: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    override fun decorate(node: ProjectViewNode<*>?, presentation: PresentationData?) {
        if (node == null || presentation == null) return
        if (node.name.isNullOrEmpty() || node.name.isNullOrBlank()) return

        val project = node.project ?: return
        val virtualFile = node.virtualFile ?: return

        if (!virtualFile.isDirectory) return
        if (!isMavenProject(virtualFile)) return

        val settings = service<ProjectInfoSettings>()

        // Extract base name by getting the directory name directly from the virtual file
        val baseName = virtualFile.name

        // Clear and set the base name
        presentation.clearText()
        presentation.addText(baseName, SimpleTextAttributes.REGULAR_ATTRIBUTES)

        // Get Maven version synchronously
        val mavenInfo = getMavenInfo(project, virtualFile, settings)

        // Get cached git info
        val gitInfo = if (settings.showGitBranch || settings.showGitTags) {
            getCachedGitInfo(virtualFile)
        } else null

        val info = buildInfoString(mavenInfo, gitInfo, settings)

        if (info.isNotEmpty()) {
            addFormattedInfo(presentation, info, settings)
        }

        // Update git info asynchronously if needed
        if ((settings.showGitBranch || settings.showGitTags) && shouldUpdateGitInfo(virtualFile)) {
            updateGitInfoAsync(project, virtualFile)
        }
    }

    private fun shouldUpdateGitInfo(virtualFile: VirtualFile): Boolean {
        val cached = gitInfoCache[virtualFile.path]
        val now = System.currentTimeMillis()
        return cached == null || (now - cached.timestamp > 30000) // 30 seconds cache
    }

    private fun getCachedGitInfo(virtualFile: VirtualFile): GitInfo? {
        return gitInfoCache[virtualFile.path]
    }

    private fun updateGitInfoAsync(project: Project, virtualFile: VirtualFile) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // Get repository in background thread
                val repository = ReadAction.compute<GitRepository?, Throwable> {
                    GitBranchUtil.guessWidgetRepository(project, virtualFile)
                } ?: return@executeOnPooledThread

                // Get branch info
                val branch = ReadAction.compute<String?, Throwable> {
                    repository.currentBranch?.name ?: repository.currentRevision?.take(7)
                }

                val settings = service<ProjectInfoSettings>()
                var tag = ""
                if (settings.showGitTags) {
                    // Get tag info
                    tag = getLatestTag(project, repository).toString()
                }

                gitInfoCache[virtualFile.path] = GitInfo(
                    branch = branch,
                    tag = tag,
                    timestamp = System.currentTimeMillis()
                )

                // Refresh UI after updating cache
                ApplicationManager.getApplication().invokeLater {
                    refreshProjectView(project)
                }
            } catch (ex: Exception) {
                logger.error(ex.message)
            }
        }
    }

    private fun refreshProjectView(project: Project) {
        ProjectView.getInstance(project).refresh()
    }

    private fun isMavenProject(virtualFile: VirtualFile): Boolean {
        return virtualFile.findChild("pom.xml") != null
    }

    private fun getMavenInfo(project: Project, virtualFile: VirtualFile, settings: ProjectInfoSettings): String? {
        if (!settings.showMavenVersion) return null

        val pomFile = virtualFile.findChild("pom.xml") ?: return null
        val mavenProjectsManager = MavenProjectsManager.getInstance(project)
        val mavenProject = mavenProjectsManager.findProject(pomFile) ?: return null

        return mavenProject.mavenId.version
    }

    private fun buildInfoString(
        mavenVersion: String?,
        gitInfo: GitInfo?,
        settings: ProjectInfoSettings
    ): String {
        val infoParts = mutableListOf<String>()

        if (mavenVersion != null) {
            infoParts.add("v$mavenVersion")
        }

        if (gitInfo != null) {
            if (settings.showGitBranch && gitInfo.branch != null) {
                infoParts.add("⎇ ${gitInfo.branch}")
            }
            if (settings.showGitTags && gitInfo.tag != null) {
                infoParts.add("⚑ ${gitInfo.tag}")
            }
        }

        return if (infoParts.isNotEmpty()) {
            "(${infoParts.joinToString(" | ")})"
        } else ""
    }

    private fun addFormattedInfo(
        presentation: PresentationData,
        info: String,
        settings: ProjectInfoSettings
    ) {
        if (!info.contains("|")) {
            // Single item, add with appropriate color and style
            val (style, color) = when {
                info.startsWith("(v") -> SimpleTextAttributes.STYLE_PLAIN to settings.getMavenVersionColorRGB()
                info.startsWith("(⎇") -> SimpleTextAttributes.STYLE_BOLD to settings.getGitBranchColorRGB()
                info.startsWith("(⚑") -> SimpleTextAttributes.STYLE_PLAIN to settings.getGitTagColorRGB()
                else -> SimpleTextAttributes.STYLE_PLAIN to SimpleTextAttributes.GRAY_ATTRIBUTES.fgColor
            }

            presentation.addText(
                " $info", SimpleTextAttributes(
                    style,
                    if (settings.useColors) color else SimpleTextAttributes.GRAY_ATTRIBUTES.fgColor
                )
            )
            return
        }

        // Multiple items
        val parts = info.substringAfter('(').substringBefore(')').split(" | ")
        presentation.addText(" (", SimpleTextAttributes.GRAY_ATTRIBUTES)

        parts.forEachIndexed { index, part ->
            if (index > 0) {
                presentation.addText(" | ", SimpleTextAttributes.GRAY_ATTRIBUTES)
            }

            val (style, color) = when {
                part.startsWith("v") -> SimpleTextAttributes.STYLE_PLAIN to settings.getMavenVersionColorRGB()
                part.startsWith("⎇") -> SimpleTextAttributes.STYLE_BOLD to settings.getGitBranchColorRGB()
                part.startsWith("⚑") -> SimpleTextAttributes.STYLE_PLAIN to settings.getGitTagColorRGB()
                else -> SimpleTextAttributes.STYLE_PLAIN to SimpleTextAttributes.GRAY_ATTRIBUTES.fgColor
            }

            presentation.addText(
                part, SimpleTextAttributes(
                    style,
                    if (settings.useColors) color else SimpleTextAttributes.GRAY_ATTRIBUTES.fgColor
                )
            )
        }

        presentation.addText(")", SimpleTextAttributes.GRAY_ATTRIBUTES)
    }

    private fun getLatestTag(project: Project, repository: GitRepository): String? {
        try {
            val handler = GitLineHandler(
                project,
                repository.root,
                GitCommand.TAG
            )

            handler.addParameters("--sort=-committerdate")
            handler.setSilent(true)

            val result = Git.getInstance().runCommand(handler)
            if (result.success() && result.output.isNotEmpty()) {
                return result.output.firstOrNull()?.trim()
            }
        } catch (ex: Exception) {
            logger.error(ex.message)
        }

        return null
    }
}
