import mu.KLogging
import java.util.stream.Stream

class Associations(private val corporaFile: String, private val stimuli: List<String>, windowSize: Int) {
    private val window = Window(windowSize)
    private val corpora: Corpora by lazy(LazyThreadSafetyMode.NONE) {
        logger.debug("Reading corpora...")
        val result = Corpora(corporaFile)
        logger.debug("Reading corpora [DONE]")
        result.saveDump()
        result
    }

    init {
        logger.info("Stimuli: ${stimuli.joinToString()}")
        logger.debug("Calculating cooccurrences...")
        calculateCooccurrences()
        logger.debug("Calculating cooccurrences [DONE]")
        logger.debug("Calculating associations...")
        calculateAssociations()
        logger.debug("Calculating associations [DONE]")
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
            .forEach {
                println("${it.first}:")
                it.second.forEach { (word, frequency) -> println("\t${word.padEnd(20)}${"%.2f".format(frequency)}") }
            }

    companion object: KLogging()
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        throw IllegalArgumentException("Invalid number of arguments. Corpora and at least one stimulus word are required.")
    }

    val corpora = args[0]
    val stimuli = args.drop(1)

    Associations(corpora, stimuli, 12)
}
