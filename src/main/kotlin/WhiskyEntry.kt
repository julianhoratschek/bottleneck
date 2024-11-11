import java.nio.file.Path
import kotlin.io.path.readText

data class WhiskyEntry(
    val name: String,
    val chillFiltered: Boolean,
    val coloured: Boolean,
    val type: String,
    val region: String,
    val distillery: String,
    val age: String,
    val abv: String,
    val casks: String
)

sealed class WhiskyEntryResult {
    data class Success(val whiskyEntry: WhiskyEntry) : WhiskyEntryResult()
    data object Failure : WhiskyEntryResult()
}

fun readWhiskyFileEx(filePath: Path): WhiskyEntryResult {
    val text = filePath.readText()
    println(text)
    val pattern = Regex("# (.*)\\s+" +
            "- \\[(.)] Kühlgefiltert\\s+" +
            "- \\[(.)] Gefärbt\\s+" +
            "- Typ: (.*)\\s+" +
            "- Region: (.*)\\s+" +
            "- Destillerie: (.*)\\s+" +
            "- Alter: (.*)\\s+" +
            "- Stärke: (.*)%.*\\s+" +
            "- Reifung: (.*)\r\n", RegexOption.MULTILINE)

    val matchResult = pattern.find(text) ?: return WhiskyEntryResult.Failure

    return WhiskyEntryResult.Success(
        WhiskyEntry(
            name = matchResult.groupValues[1],
            chillFiltered = matchResult.groupValues[2] != " ",
            coloured = matchResult.groupValues[3] != " ",
            type = matchResult.groupValues[4],
            region = matchResult.groupValues[5],
            distillery = matchResult.groupValues[6].trim('[', ']'),
            age = matchResult.groupValues[7],
            abv = matchResult.groupValues[8].replace(',', '.'),
            casks = matchResult.groupValues[9]))
}