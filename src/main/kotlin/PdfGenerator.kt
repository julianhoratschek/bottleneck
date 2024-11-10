import java.nio.file.Path
import kotlin.io.path.*

sealed class SelectResult {
    data object Success : SelectResult()
    data object FileNotFound : SelectResult()
    data class MissingFiles(val missingPaths: List<Path>) : SelectResult()
}


class PdfGenerator(private val config: Configuration) {

    private val templateText = this.javaClass.getResource("template.tex")
        ?.readText()
        ?: "% bottleneck_insert"

    private val texIncludeDirectory = this.javaClass.getResource("whisxy.sty")
        ?.toURI()?.toPath()
        ?: Path(".")

    private val _selectWhisky = mutableListOf<Path>()

    private var fileName: String = "out"

    fun selectFromTastingFile(tastingFile: Path): SelectResult {
        if(tastingFile.notExists())
            return SelectResult.FileNotFound

        val foundFiles = Regex("""- \[\[([^]]+)]]""")
            .findAll(tastingFile.readText())
            .map { config.vault / "${it.groupValues[1]}.md" }
            .groupBy { it.exists() }

        val missing = foundFiles[false]
        if (missing != null)
            return SelectResult.MissingFiles(missing)

        fileName = tastingFile.nameWithoutExtension

        _selectWhisky.clear()
        _selectWhisky.addAll(foundFiles[true] ?: listOf())

        return SelectResult.Success
    }

    fun generateLatexFile() : Path? {
        if (_selectWhisky.isEmpty())
            return null

        val insertText = _selectWhisky
            .mapNotNull { readWhiskyFile(it) }
            .joinToString("\n") {
                """\whiskey{${it.name}}
                |{${it.type}}
                |{${it.abv}}
                |{${if (it.chillFiltered) "Kühlgefiltert" else "Nicht kühlgefiltert"}, ${if (it.coloured) "mit Zuckercouleur" else "ohne Farbstoff"}}
                |{${it.distillery} (${it.region})}
                |{${it.age}}
                |{${it.casks}}""".trimMargin()
            }

        val texFile = config.output / "$fileName.tex"

        if(!texFile.exists())
            texFile.createFile()

        return texFile.apply { writeText(templateText.replace("% bottleneck_insert", insertText)) }
    }

    fun generatePdf() {
        ProcessBuilder("pdflatex", "-synctex=1", "-output-format=pdf",
            "-output-directory=${config.output / "out"}", "-aux-directory=${config.output / "aux"}",
            "-include-directory=${texIncludeDirectory.parent}",
            (config.output / "$fileName.tex").toString())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor()
    }
}