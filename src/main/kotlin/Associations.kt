import mu.KLogging
import java.io.File
import java.util.stream.Stream

class Associations(output: String, private val corporaFile: String, private val stimuli: List<String>, windowSize: Int) {
    private val outputDirectory = File(output)
    private val window = Window(windowSize)
    private val corpora: Corpora by lazy(LazyThreadSafetyMode.NONE) {
        logger.debug("Reading corpora...")
        val result = Corpora(corporaFile)
        logger.debug("Reading corpora [DONE]")
        result.saveDump()
        result
    }

    init {
        outputDirectory.mkdirs()
        logger.info("Stimuli: ${stimuli.joinToString()}")
        logger.debug("Calculating cooccurrences...")
        calculateCooccurrences()
        logger.debug("Calculating cooccurrences [DONE]")
        logger.debug("Calculating associations...")
        val associations = calculateAssociations()
        logger.debug("Calculating associations [DONE]")
        logger.debug("Saving associations...")
        saveAssociations(associations)
        logger.debug("Saving associations [DONE]")
    }

    private fun calculateCooccurrences() = Stream.concat(
            corpora.words(),
            List<String?>(window.size, { null }).stream()
    ).forEach {
        val currentWord = window.currentWord()
        if (currentWord != null && stimuli.contains(currentWord)) {
            corpora.updateCoOccurences(currentWord, window.words(currentWord))
        }
        window.slide(it)
    }

    private fun calculateAssociations() = stimuli
            .filter(corpora::has)
            .map(corpora::associationsFor)

    private fun saveAssociations(associations: List<Pair<String, List<Pair<String, Double>>>>) {
        associations.forEach {
            File(outputDirectory, "${it.first}.txt").printWriter().use { out ->
                it.second.forEach { (word, frequency) -> out.println("${word.padEnd(20)}${"%.2f".format(frequency)}") }
            }
        }
    }

    companion object : KLogging()
}

fun main(args: Array<String>) {
    if (args.size < 3) {
        throw IllegalArgumentException("Invalid number of arguments. Output directory, corpora and at least one stimulus word are required.")
    }

    val output = args[0]
    val corpora = args[1]
    val stimuli = args.drop(2)

    Associations(output, corpora, stimuli, 12)
}
