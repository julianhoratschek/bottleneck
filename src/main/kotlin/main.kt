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

fun printHelp() {
    println("Usage:\nbottleneck [-v|--vault <vault_path>] [-o|--output-dir <output_dir>] [-t|-no-pdf] [-h|--help] <tasting_file>")
    println("\t--vault <vault_path>: Load all data from <vault_path>")
    println("\t--output-dir <output_dir>: Save all output files into <output_dir>")
    println("\t--no-pdf: Does not generate a pdf after successfully creating tex-files")
    println("\t--help: Prints this help message")
    println("\t<tasting_file>: File to read all elements from")
}

fun main(args: Array<String>) {
    val configs = ConfigurationLoader()

    val argIterator = args.iterator()
    var tastingFile: String? = null
    var generatePdf = true

    while (argIterator.hasNext()) {
        when (val arg = argIterator.next()) {
            "-v", "--vault" -> argParam(argIterator)?.let { configs.config.vaultPath = it } ?: return
            "-o", "--out", "--output-dir" -> argParam(argIterator)?.let { configs.config.outputDirectory = it} ?: return
            "-n", "--no-pdf", "--tex-only" -> generatePdf = false
            "-h", "--help" -> return printHelp()
            else -> {
                if (arg[0] == '-') {
                    println("Unknown parameter: $arg")
                    return
                }

                tastingFile = arg
            }
        }
    }

    println("Vault Directory: ${configs.config.vault}")
    println("Output Directory: ${configs.config.output}")

    configs.config.update()
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
            if (pdfGenerator.generateLatexFile() == null) {
                println("Could not generate file")
                return
            }
        }
    }

    if (generatePdf) {
        println("Generating PDF...")
        pdfGenerator.generatePdf()
    }
}