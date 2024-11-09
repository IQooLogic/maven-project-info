package rs.devlabs.maveninfo

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.CheckBox
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import java.awt.Color
import javax.swing.JComponent

class SimpleProjectInfoSettingsConfigurable : Configurable {
    private var settingsPanel: DialogPanel? = null
    private val settings = service<SimpleProjectInfoSettings>()

    // initial values
    var initialEnabled: Boolean = true
    var initialShowProjectVersion: Boolean = true
    var initialShowGitBranch: Boolean = false
    var initialUseColors: Boolean = true
    var initialProjectVersionColor: String = "#CC7832"  // Default: orange
    var initialGitBranchColor: String = "#6897BB"     // Default: blue

    private val showProjectVersionCheckBox = CheckBox("Show project version")
    private val showGitBranchCheckBox = CheckBox("Show git branch")
    private val showUseColorsCheckBox = CheckBox("Use colors")
    private val projectVersionColorPanel = ColorPanel()
    private val gitBranchColorPanel = ColorPanel()

    override fun getDisplayName(): @NlsContexts.ConfigurableName String? {
        return "Project Info Settings"
    }

    override fun createComponent(): JComponent? {
        // set initial state
        initialEnabled = settings.enabled
        initialShowProjectVersion = settings.showProjectVersion
        initialShowGitBranch = settings.showGitBranch
        initialUseColors = settings.useColors
        initialProjectVersionColor = settings.projectVersionColor
        initialGitBranchColor = settings.gitBranchColor

        // Initialize color panels with current values
        projectVersionColorPanel.selectedColor = Color.decode(settings.projectVersionColor)
        gitBranchColorPanel.selectedColor = Color.decode(settings.gitBranchColor)

        settingsPanel = panel {
            group("Display Options") {
                row {
                    cell(showProjectVersionCheckBox).bindSelected(
                        { settings.showProjectVersion },
                        {
                            settings.showProjectVersion = it
                        }
                    )
                }
//                row {
//                    checkBox("Show project version")
//                        .bindSelected(
//                            { settings.showProjectVersion },
//                            {
//                                settings.showProjectVersion = it
//                            }
//                        )
//                }
                row {
                    cell(showGitBranchCheckBox).bindSelected(
                        { settings.showGitBranch },
                        {
                            settings.showGitBranch = it
                        }
                    )
                }
//                row {
//                    checkBox("Show git branch")
//                        .bindSelected(
//                            { settings.showGitBranch },
//                            {
//                                settings.showGitBranch = it
//                            }
//                        )
//                }
                row {
                    cell(showUseColorsCheckBox).bindSelected(
                        { settings.useColors },
                        {
                            settings.useColors = it
                        }
                    )
                }
//                row {
//                    checkBox("Use colors")
//                        .bindSelected(
//                            { settings.useColors },
//                            {
//                                settings.useColors = it
//                            }
//                        )
//                }
            }

            group("Color Settings") {
                row("Project Version: ") {
                    cell(projectVersionColorPanel)
                        .component
                }
                row("Git Branch: ") {
                    cell(gitBranchColorPanel)
                        .component
                }
            }

            row {
                button("Reset to Defaults") {
                    settings.resetToDefaults()
                    reset()
                }
            }
        }

        return settingsPanel!!
    }

    override fun isModified(): Boolean {
        return settingsPanel?.isModified() == true ||
                initialShowProjectVersion != showProjectVersionCheckBox.isSelected ||
                initialShowGitBranch != showGitBranchCheckBox.isSelected ||
                initialUseColors != showUseColorsCheckBox.isSelected ||
                projectVersionColorPanel.selectedColor != Color.decode(settings.projectVersionColor) ||
                gitBranchColorPanel.selectedColor != Color.decode(settings.gitBranchColor)
    }

    override fun apply() {
        settingsPanel?.apply()

        projectVersionColorPanel.selectedColor?.let {
            settings.projectVersionColor = "#%02X%02X%02X".format(it.red, it.green, it.blue)
        }
        gitBranchColorPanel.selectedColor?.let {
            settings.gitBranchColor = "#%02X%02X%02X".format(it.red, it.green, it.blue)
        }

        refreshProjectView()
    }

    override fun reset() {
        settings.showProjectVersion = initialShowProjectVersion
        settings.showGitBranch = initialShowGitBranch
        settings.useColors = initialUseColors
        settings.projectVersionColor = initialProjectVersionColor
        settings.gitBranchColor = initialGitBranchColor

        projectVersionColorPanel.selectedColor = Color.decode(settings.projectVersionColor)
        gitBranchColorPanel.selectedColor = Color.decode(settings.gitBranchColor)
    }

    private fun refreshProjectView() {
        ProjectManager.getInstance().openProjects.forEach { project ->
            ProjectView.getInstance(project).refresh()
        }
    }
}
