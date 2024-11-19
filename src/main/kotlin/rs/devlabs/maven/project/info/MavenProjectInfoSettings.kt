package rs.devlabs.maven.project.info

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.awt.Color

@State(
    name = "MavenProjectInfoSettings",
    storages = [Storage("maven-info-settings.xml")]
)
class MavenProjectInfoSettings : PersistentStateComponent<MavenProjectInfoSettings> {
    var enabled: Boolean = true
    var showProjectVersion: Boolean = true
    var showGitBranch: Boolean = false
    var useColors: Boolean = true
    var projectVersionColor: String = "#CC7832"  // Default: orange
    var gitBranchColor: String = "#6897BB"     // Default: blue

    fun getProjectVersionColorRGB(): Color = Color.decode(projectVersionColor)
    fun getGitBranchColorRGB(): Color = Color.decode(gitBranchColor)

    fun resetToDefaults() {
        enabled = true
        showProjectVersion = true
        showGitBranch = false
        useColors = true
        projectVersionColor = "#CC7832"
        gitBranchColor = "#6897BB"
    }

    override fun getState(): MavenProjectInfoSettings = this

    override fun loadState(state: MavenProjectInfoSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
