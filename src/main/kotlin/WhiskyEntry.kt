import java.nio.file.Path
import kotlin.io.path.readLines

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

fun readWhiskyFile(filePath: Path): WhiskyEntry? {
    val lines = filePath.readLines().filter { it.startsWith("- ") or it.startsWith("# ") }

    if (lines.size < 9) {
        println("Warning: Cannot read file $filePath")
        return null
    }

    return WhiskyEntry(
        name = lines[0].substring(2),
        chillFiltered = lines[1][3] != ' ',
        coloured = lines[2][3] != ' ',
        type = lines[3].substring(7),
        region = lines[4].substring(10),
        distillery = lines[5].substring(15).trim('[', ']'),
        age = lines[6].substring(9),
        abv = lines[7].substring(10).removeSuffix("% vol.").trim().replace(',', '.'),
        casks = lines[8].substring(11)
    )
}