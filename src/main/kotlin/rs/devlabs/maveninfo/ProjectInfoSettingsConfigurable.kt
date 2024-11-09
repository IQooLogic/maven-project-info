package rs.devlabs.maveninfo

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ColorPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import java.awt.Color
import javax.swing.JComponent

class ProjectInfoSettingsConfigurable : Configurable {
    private var myPanel: DialogPanel? = null
    private val settings = service<ProjectInfoSettings>()

    // Store initial values
    private var initialMavenVersion = false
    private var initialGitBranch = false
    private var initialGitTags = false
    private var initialUseColors = false
    private var initialMavenColor = ""
    private var initialBranchColor = ""
    private var initialTagColor = ""

    // Delay refresh to avoid multiple updates
    private var refreshTimer: javax.swing.Timer? = null

    private fun scheduleRefresh() {
        refreshTimer?.stop()
        refreshTimer = javax.swing.Timer(300) { _ ->
            refreshProjectView()
        }.apply {
            isRepeats = false
            start()
        }
    }

    private val mavenVersionColorPanel = ColorPanel().apply {
        addActionListener {
            scheduleRefresh()
        }
    }

    private val gitBranchColorPanel = ColorPanel().apply {
        addActionListener {
            scheduleRefresh()
        }
    }

    private val gitTagColorPanel = ColorPanel().apply {
        addActionListener {
            scheduleRefresh()
        }
    }

    override fun getDisplayName(): String = "Project Info Display Settings"

    override fun createComponent(): JComponent {
        // Capture initial state
        initialMavenVersion = settings.showMavenVersion
        initialGitBranch = settings.showGitBranch
        initialGitTags = settings.showGitTags
        initialUseColors = settings.useColors
        initialMavenColor = settings.mavenVersionColor
        initialBranchColor = settings.gitBranchColor
        initialTagColor = settings.gitTagColor

        // Initialize color panels with current values
        mavenVersionColorPanel.selectedColor = Color.decode(settings.mavenVersionColor)
        gitBranchColorPanel.selectedColor = Color.decode(settings.gitBranchColor)
        gitTagColorPanel.selectedColor = Color.decode(settings.gitTagColor)

        myPanel = panel {
            group("Display Options") {
                row {
                    checkBox("Show maven project version")
                        .bindSelected(
                            { settings.showMavenVersion },
                            {
                                settings.showMavenVersion = it
                                scheduleRefresh()
                            }
                        )
                }
                row {
                    checkBox("Show git branch")
                        .bindSelected(
                            { settings.showGitBranch },
                            {
                                settings.showGitBranch = it
                                scheduleRefresh()
                            }
                        )
                }
                row {
                    checkBox("Show git latest tags")
                        .bindSelected(
                            { settings.showGitTags },
                            {
                                settings.showGitTags = it
                                scheduleRefresh()
                            }
                        )
                }
                row {
                    checkBox("Use colors")
                        .bindSelected(
                            { settings.useColors },
                            {
                                settings.useColors = it
                                scheduleRefresh()
                            }
                        )
                }
            }

            group("Color Settings") {
                row("Maven Version: ") {
                    cell(mavenVersionColorPanel)
                        .component
                }
                row("Git Branch: ") {
                    cell(gitBranchColorPanel)
                        .component
                }
                row("Git Tag: ") {
                    cell(gitTagColorPanel)
                        .component
                }
            }

            row {
                button("Reset to Defaults") {
                    settings.resetToDefaults()
                    resetColors()
                    scheduleRefresh()
                }
            }
        }
        return myPanel!!
    }

    private fun resetColors() {
        mavenVersionColorPanel.selectedColor = Color.decode(settings.mavenVersionColor)
        gitBranchColorPanel.selectedColor = Color.decode(settings.gitBranchColor)
        gitTagColorPanel.selectedColor = Color.decode(settings.gitTagColor)
    }

    private fun refreshProjectView() {
        ProjectManager.getInstance().openProjects.forEach { project ->
            ProjectView.getInstance(project).refresh()
        }
    }

    override fun isModified(): Boolean {
        return myPanel?.isModified() ?: false ||
                mavenVersionColorPanel.selectedColor != Color.decode(settings.mavenVersionColor) ||
                gitBranchColorPanel.selectedColor != Color.decode(settings.gitBranchColor) ||
                gitTagColorPanel.selectedColor != Color.decode(settings.gitTagColor)
    }

    override fun apply() {
        myPanel?.apply()

        // Apply color changes only when Apply is clicked
        mavenVersionColorPanel.selectedColor?.let {
            settings.mavenVersionColor = "#%02X%02X%02X".format(it.red, it.green, it.blue)
        }
        gitBranchColorPanel.selectedColor?.let {
            settings.gitBranchColor = "#%02X%02X%02X".format(it.red, it.green, it.blue)
        }
        gitTagColorPanel.selectedColor?.let {
            settings.gitTagColor = "#%02X%02X%02X".format(it.red, it.green, it.blue)
        }

        scheduleRefresh()
    }

    override fun reset() {
        // Restore initial values from when the dialog was opened
        settings.showMavenVersion = initialMavenVersion
        settings.showGitBranch = initialGitBranch
        settings.showGitTags = initialGitTags
        settings.useColors = initialUseColors
        settings.mavenVersionColor = initialMavenColor
        settings.gitBranchColor = initialBranchColor
        settings.gitTagColor = initialTagColor

        myPanel?.reset()
        resetColors()
        scheduleRefresh()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return myPanel
    }
}
