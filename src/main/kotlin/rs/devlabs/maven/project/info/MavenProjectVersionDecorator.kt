package rs.devlabs.maven.project.info

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.idea.maven.project.MavenProjectsManager

class MavenProjectVersionDecorator : ProjectViewNodeDecorator {

    private val settings = service<MavenProjectInfoSettings>()

    override fun decorate(
        node: ProjectViewNode<*>?,
        presentation: PresentationData?
    ) {
        if (!settings.showProjectVersion || !settings.enabled) return
        if (node == null || presentation == null) return
        if (node.name.isNullOrEmpty() || node.name.isNullOrBlank()) return

        val project = node.project ?: return
        val virtualFile = node.virtualFile ?: return

        if (!virtualFile.isDirectory) return
        if (!isMavenProject(virtualFile)) return

        val version = getMavenInfo(project, virtualFile)
        version.let {
            presentation.clearText()

            var name = virtualFile.name
            if (isRootNode(project, virtualFile)) {
                name = project.name
            }
            // name
            presentation.addText(
                name, SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_PLAIN,
                    SimpleTextAttributes.REGULAR_ATTRIBUTES.fgColor
                ))

            // version
            presentation.addText(" v$it", SimpleTextAttributes(
                SimpleTextAttributes.STYLE_PLAIN,
                if (settings.useColors) settings.getProjectVersionColorRGB() else SimpleTextAttributes.GRAY_ATTRIBUTES.fgColor
            ))
        }
    }

    private fun isMavenProject(virtualFile: VirtualFile): Boolean {
        return virtualFile.findChild("pom.xml") != null
    }

    private fun isRootNode(project: Project, virtualFile: VirtualFile): Boolean {
        return project.basePath == virtualFile.path
    }

    private fun getMavenInfo(project: Project, virtualFile: VirtualFile): String? {
        val pomFile = virtualFile.findChild("pom.xml") ?: return null
        val mavenProjectsManager = MavenProjectsManager.getInstance(project)
        val mavenProject = mavenProjectsManager.findProject(pomFile) ?: return null

        return mavenProject.mavenId.version
    }
}
