import mu.KLogging
import java.util.stream.Stream

class App(private val corpora: Corpora, private val stimuli: List<String>, private val window: Window) {
    init {
        logger.info("Stimuli: ${stimuli.joinToString()}")
        logger.debug("Calculating cooccurrences...")
        calculateCooccurrences()
        logger.debug("Displaying associations...")
        displayAssociations()
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

    private fun displayAssociations() = stimuli
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

    val corpora = Corpora(args[0])
    val stimuli = args.drop(1)
    val window = Window(12)

    App(corpora, stimuli, window)
}
