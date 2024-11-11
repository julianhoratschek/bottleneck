import java.nio.file.Path
import kotlin.io.path.*


sealed class SelectResult {
    data object Success : SelectResult()
    data object FileNotFound : SelectResult()
    data object NoFilesFound : SelectResult()
    data class MissingFiles(val missingPaths: List<Path>) : SelectResult()
}

sealed class GenerateLatexResult {
    data object Success : GenerateLatexResult()
    data class BadFiles(val files: List<Path>) : GenerateLatexResult()
}


class PdfGenerator(private val config: Configuration) {

    private val templateText = this.javaClass.getResource("template.tex")
        ?.readText()
        ?: "% bottleneck_insert"

    private val texIncludeDirectory = this.javaClass.getResource("whisxy.sty")
        ?.toURI()?.toPath()?.parent
        ?: Path(".")

    private val _selectWhisky = mutableListOf<Path>()

    private var fileName: String = "out"

    /**
     * Reads a list of obsidian links like:
     * - [[The Whiskey Name]]
     * from tastingFile and ensures all the linked files exist.
     */
    fun selectFromTastingFile(tastingFile: Path): SelectResult {
        if(tastingFile.notExists())
            return SelectResult.FileNotFound

        // - [[...]] is an obsidian link, we only want the ... part
        val foundFiles = Regex("""- \[\[([^]]+)]]""")
            .findAll(tastingFile.readText())
            .map { config.vault / "${it.groupValues[1]}.md" }
            .groupBy { it.exists() }

        // Return if any file could not be found
        val missing = foundFiles[false]
        if (missing != null)
            return SelectResult.MissingFiles(missing)

        // Return if we haven't got any file
        val found = foundFiles[true] ?: return SelectResult.NoFilesFound

        fileName = tastingFile.nameWithoutExtension
        _selectWhisky.clear()
        _selectWhisky.addAll(found)

        return SelectResult.Success
    }

    fun generateLatexFile(): GenerateLatexResult {
        val badFiles = mutableListOf<Path>()
        val insertText = _selectWhisky
            .mapNotNull {
                when(val res = readWhiskyFileEx(it)) {
                    is WhiskyEntryResult.Success -> res.whiskyEntry
                    is WhiskyEntryResult.Failure -> {
                        badFiles.add(it)
                        null
                    }
                }
            }
            .joinToString("\n") {
                """\whiskey{${it.name}}
                |{${it.type}}
                |{${it.abv}}
                |{${if (it.chillFiltered) "Kühlgefiltert" else "Nicht kühlgefiltert"}, ${if (it.coloured) "mit Zuckercouleur" else "ohne Farbstoff"}}
                |{${it.distillery} (${it.region})}
                |{${it.age}}
                |{${it.casks}}
                |
                |""".trimMargin()
            }

        val texFile = config.output / "$fileName.tex"

        if(!texFile.exists())
            texFile.createFile()

        texFile.writeText(templateText.replace("% bottleneck_insert", insertText))

        return if (badFiles.isEmpty())
            GenerateLatexResult.Success
        else GenerateLatexResult.BadFiles(badFiles)
    }

    fun generatePdf() {
        ProcessBuilder("pdflatex", "-synctex=1", "-output-format=pdf",
            "-output-directory=${config.output / "out"}", "-aux-directory=${config.output / "aux"}",
            "-include-directory=${texIncludeDirectory}",
            (config.output / "$fileName.tex").toString())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor()
    }
}