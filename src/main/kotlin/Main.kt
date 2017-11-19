import java.util.stream.Stream

fun main(args: Array<String>) {
    if (args.size < 2) {
        throw IllegalArgumentException("Invalid number of arguments. Corpus and at least one stimulus word are required.")
    }

    val corpus = Corpus(args[0])
    val stimuli = args.drop(1)
    val window = Window(12)

    calculateCoocurences(stimuli, corpus, window)
    displayAssociantions(stimuli, corpus)
}

fun calculateCoocurences(stimuli: List<String>, corpus: Corpus, window: Window) = Stream.concat(
        corpus.words(),
        List<String?>(window.size, { null }).stream()
).forEach {
    val currentWord = window.currentWord()
    if (currentWord != null && stimuli.contains(currentWord)) {
        corpus.updateCoOccurences(currentWord, window)
    }
    window.slide(it)
}

fun displayAssociantions(stimuli: List<String>, corpus: Corpus) = stimuli
        .filter(corpus::has)
        .map(corpus::associationsFor)
        .forEach {
            println("${it.first}:")
            it.second.forEach { (word, frequency) -> println("\t${word.padEnd(20)}${"%.2f".format(frequency)}") }
        }
