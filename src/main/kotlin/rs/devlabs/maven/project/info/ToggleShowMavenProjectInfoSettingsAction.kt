package rs.devlabs.maven.project.info

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service

class ToggleShowMavenProjectInfoSettingsAction : ToggleAction("Toggle Maven Project Info") {
    private val settings = service<MavenProjectInfoSettings>()

    override fun isSelected(e: AnActionEvent): Boolean {
        return settings.enabled
    }

    override fun setSelected(e: AnActionEvent, enabled: Boolean) {
        settings.enabled = enabled
        // Refresh project view directly here
        e.project?.let { project ->
            ProjectView.getInstance(project)?.refresh()
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
