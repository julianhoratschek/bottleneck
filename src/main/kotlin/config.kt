import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

@Serializable
data class Configuration(
    var vaultPath: String = ".",
    var outputDirectory: String = "."
) {
    val vault: Path
        get() = Paths.get(vaultPath).normalize()
    val output: Path
        get() = Paths.get(outputDirectory).normalize()

    fun update() {
        vaultPath = vault.toString()
        outputDirectory = output.toString()
    }
}

class ConfigurationLoader {
    private val configFile = Path(System.getenv("LOCALAPPDATA") ?: ".") / "bottleneck/config.json"

    init {
        if (!configFile.exists()) {
            configFile.createParentDirectories()
            configFile.createFile()
            configFile.writeText("{}")
        }
    }

    val config: Configuration = Json.Default.decodeFromString(configFile.readText())

    fun save() = configFile.writeText(Json.Default.encodeToString(config))
}