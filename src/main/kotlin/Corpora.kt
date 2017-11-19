import morfologik.stemming.polish.PolishStemmer
import mu.KLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.streams.asSequence

class Corpora(private val filename: String) {
    private val alpha = 0.66
    private val beta = 0.00002
    private val gamma = 0.00002
    private val numberOfAssociations = 10

    private val stemmer = PolishStemmer()
    private val cooccurrences: MutableMap<Pair<String, String>, Int> = mutableMapOf()
    private val occurrences: Map<String, Int> by lazy(LazyThreadSafetyMode.NONE) {
        logger.debug("Counting occurrences...")
        val result = words().asSequence().groupBy { it }.map {
            Pair(it.key, it.value.count())
        }.toMap()
        logger.debug("Counting occurrences [DONE]")
        result
    }
    private val size: Long by lazy(LazyThreadSafetyMode.NONE) {
        logger.debug("Counting words...")
        val result = words().count()
        logger.debug("Counting words [DONE]")
        result
    }

    init {
        logger.info("File: $filename")
        logger.info("Size: $size words")
    }

    private val betaSize: Double by lazy(LazyThreadSafetyMode.NONE) {
        beta * size
    }
    private val gammaSize: Double by lazy(LazyThreadSafetyMode.NONE) {
        gamma * size
    }
    private val sizeToAlpha: Double by lazy(LazyThreadSafetyMode.NONE) {
        Math.pow(size.toDouble(), alpha)
    }

    fun words(): Stream<String> = File(filename)
            .bufferedReader()
            .lines()
            .map { it.split(Regex("\\s+")) }
            .flatMap { it.stream() }
            .map(this::transform)
            .filter(CharSequence::isNotEmpty)

    fun updateCoOccurences(stimulus: String, window: Window) {
        window.words(stimulus).forEach {
            val pair = Pair(stimulus, it!!)
            val occurrences = cooccurrences.getOrDefault(pair, 0)
            cooccurrences[pair] = occurrences + 1
        }
    }

    fun has(word: String) = occurrences.containsKey(word)

    fun associationsFor(stimulus: String): Pair<String, List<Pair<String, Double>>> {
        logger.debug("Calculating associations for $stimulus...")
        val result = Pair(stimulus, occurrences.keys
                .stream()
                .map { Pair(it, calculateStrength(stimulus, it)) }
                .asSequence()
                .sortedBy { it.second }
                .take(numberOfAssociations)
                .toList()
        )
        logger.debug("Calculating associations for $stimulus [DONE]")
        return result
    }

    private fun transform(word: String) = stem(word.toLowerCase().replace(Regex("[^a-ząćęłóńśżź]"), ""))

    private fun stem(word: String): String {
        val stem = stemmer.lookup(word)
        return if (stem.size > 0) stem[0].stem.toString() else word
    }

    private fun calculateStrength(stimulus: String, word: String) =
            sizeToAlpha / occurrences[stimulus]!! *
                    cooccurrences.getOrDefault(Pair(stimulus, word), 0) / weaken(word)

    private fun weaken(word: String): Double {
        val occurrence = occurrences[word]!!
        return if (occurrence > betaSize) Math.pow(occurrence.toDouble(), alpha) else gammaSize
    }

    companion object: KLogging()
}
