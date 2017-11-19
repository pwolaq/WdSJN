import mu.KLogging
import java.util.stream.Stream

class App(private val corporaFile: String, private val stimuli: List<String>, windowSize: Int) {
    private val window = Window(windowSize)
    private val corpora: Corpora by lazy(LazyThreadSafetyMode.NONE) {
        logger.debug("Reading corpora...")
        Corpora(corporaFile)
    }

    init {
        logger.info("Stimuli: ${stimuli.joinToString()}")
        logger.debug("Calculating cooccurrences...")
        calculateCooccurrences()
        logger.debug("Calculating associations...")
        calculateAssociations()
    }

    private fun calculateCooccurrences() = Stream.concat(
            corpora.words(),
            List<String?>(window.size, { null }).stream()
    ).forEach {
        val currentWord = window.currentWord()
        if (currentWord != null && stimuli.contains(currentWord)) {
            corpora.updateCoOccurences(currentWord, window)
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

    App(args[0], args.drop(1), 12)
}
