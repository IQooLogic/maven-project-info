package rs.devlabs.maveninfo

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.awt.Color

@State(
    name = "ProjectInfoSettings",
    storages = [Storage("project-info-settings.xml")]
)
class ProjectInfoSettings : PersistentStateComponent<ProjectInfoSettings> {
    var showMavenVersion: Boolean = true
    var showGitBranch: Boolean = false
    var showGitTags: Boolean = false
    var useColors: Boolean = true
    var mavenVersionColor: String = "#6A8759"  // Default: green
    var gitBranchColor: String = "#CC7832"     // Default: orange
    var gitTagColor: String = "#6897BB"        // Default: blue

    fun getMavenVersionColorRGB(): Color = Color.decode(mavenVersionColor)
    fun getGitBranchColorRGB(): Color = Color.decode(gitBranchColor)
    fun getGitTagColorRGB(): Color = Color.decode(gitTagColor)

    fun resetToDefaults() {
        showMavenVersion = true
        showGitBranch = false
        showGitTags = false
        useColors = true
        mavenVersionColor = "#6A8759"
        gitBranchColor = "#CC7832"
        gitTagColor = "#6897BB"
    }

    override fun getState(): ProjectInfoSettings = this

    override fun loadState(state: ProjectInfoSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
