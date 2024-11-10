import kotlin.io.path.Path

fun argParam(argIterator: Iterator<String>) : String? {
    if (!argIterator.hasNext()) {
        println("Expected parameter after $argIterator")
        return null
    }

    val arg = argIterator.next()
    if (arg.first() == '-') {
        println("Parameter cannot start with '-': $arg")
        return null
    }

    return arg
}

fun main(args: Array<String>) {
    val configs = ConfigurationLoader()

    val argIterator = args.iterator()
    var tastingFile: String? = null
    var generate_pdf = false

    while (argIterator.hasNext()) {
        when (val arg = argIterator.next()) {
            "-v", "--vault" -> argParam(argIterator)?.let { configs.config.vaultPath = it } ?: return
            "-o", "--out", "--output-dir" -> argParam(argIterator)?.let { configs.config.outputDirectory = it} ?: return
            "-p", "--pdf" -> generate_pdf = true
            else -> {
                if (tastingFile != null)
                    println("Warning: Overwriting tasting File ($tastingFile)")
                tastingFile = arg
            }
        }
    }

    println("Vault Directory: ${configs.config.vault}")
    println("Output Directory: ${configs.config.output}")
    configs.save()
    println("Configurations saved")

    if (tastingFile == null) {
        println("No tasting file was set")
        return
    }

    val pdfGenerator = PdfGenerator(configs.config)

    when(val res = pdfGenerator.selectFromTastingFile(Path(tastingFile))) {
        is SelectResult.FileNotFound -> println("File not found: $tastingFile")
        is SelectResult.MissingFiles -> println("Missing files:\n${res.missingPaths.joinToString("\n")}")
        is SelectResult.Success -> {
            println("Generating: TEX in ${configs.config.output}")
            pdfGenerator.generateLatexFile()
        }
    }

    if (generate_pdf) {
        println("Generating PDF...")
        pdfGenerator.generatePdf()
    }
}