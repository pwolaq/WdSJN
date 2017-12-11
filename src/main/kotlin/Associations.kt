import mu.KLogging
import java.io.File
import java.io.PrintWriter
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
        prepareOutputDirectory()
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

        logger.debug("Extracting sentences...")
        extractSentences(associations)
        logger.debug("Extracting sentences [DONE]")
    }

    private fun prepareOutputDirectory() {
        if (outputDirectory.isDirectory) {
            outputDirectory.deleteRecursively()
            outputDirectory.mkdirs()
        }
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

    private fun extractSentences(associations: List<Pair<String, List<Pair<String, Double>>>>) {
        val words = mapWordsToStimuli(associations)
        val files = mapWordsToWriters(words)

        extractSentencesToFiles(words, files)
    }

    private fun extractSentencesToFiles(words: Map<String, List<String>>, files: Map<Pair<String, String>, PrintWriter>) {
        val window = Window(20)

        Stream.concat(
                corpora.originalWords(),
                List<String?>(window.size, { null }).stream()
        ).forEach {
            val currentWord = corpora.stem(window.currentWord() ?: "")
            if (words.containsKey(currentWord)) {
                val sentence = window.sentence()
                window.words(currentWord).map(
                        corpora::stem
                ).forEach {
                    if (words[currentWord]!!.contains(it)) {
                        files[Pair(it, currentWord)]!!.println(sentence)
                    }
                }
            }
            window.slide(it)
        }

        files.forEach {
            it.value.close()
        }
    }

    private fun mapWordsToStimuli(associations: List<Pair<String, List<Pair<String, Double>>>>): Map<String, List<String>> {
        val words: MutableMap<String, MutableList<String>> = mutableMapOf()

        associations.forEach {
            val stimulus = it.first
            it.second.forEach {
                if (words.containsKey(it.first)) {
                    words[it.first]!!.add(stimulus)
                } else {
                    words[it.first] = mutableListOf(stimulus)
                }
            }
        }

        return words
    }

    private fun mapWordsToWriters(words: Map<String, List<String>>) =
            words.flatMap {
                val word = it.key
                it.value.map {
                    Pair(it, word)
                }
            }.map {
                it to File(outputDirectory, "${it.first}-${it.second}.txt").printWriter()
            }.toMap()

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
