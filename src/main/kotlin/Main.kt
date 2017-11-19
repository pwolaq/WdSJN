import java.util.stream.Stream

fun main(args: Array<String>) {
    if (args.size < 2) {
        throw IllegalArgumentException("Invalid number of arguments. Corpora and at least one stimulus word are required.")
    }

    val corpora = Corpora(args[0])
    val stimuli = args.drop(1)
    val window = Window(12)

    calculateCooccurrences(stimuli, corpora, window)
    displayAssociations(stimuli, corpora)
}

fun calculateCooccurrences(stimuli: List<String>, corpora: Corpora, window: Window) = Stream.concat(
        corpora.words(),
        List<String?>(window.size, { null }).stream()
).forEach {
    val currentWord = window.currentWord()
    if (currentWord != null && stimuli.contains(currentWord)) {
        corpora.updateCoOccurences(currentWord, window)
    }
    window.slide(it)
}

fun displayAssociations(stimuli: List<String>, corpora: Corpora) = stimuli
        .filter(corpora::has)
        .map(corpora::associationsFor)
        .forEach {
            println("${it.first}:")
            it.second.forEach { (word, frequency) -> println("\t${word.padEnd(20)}${"%.2f".format(frequency)}") }
        }
