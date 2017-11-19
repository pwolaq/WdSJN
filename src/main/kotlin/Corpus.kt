import morfologik.stemming.polish.PolishStemmer
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.streams.asSequence

class Corpus(private val filename: String) {
    private val stemmer = PolishStemmer()
    private val coocurrences: MutableMap<Pair<String, String>, Int> = mutableMapOf()
    private val occurences = words().asSequence().groupBy { it }.map {
        Pair(it.key, it.value.count())
    }.toMap()

    private val size = words().count()
    private val alpha = 0.66
    private val beta = 0.00002
    private val gamma = 0.00002

    private val betaSize = beta * size
    private val gammaSize = gamma * size
    private val sizeToAlpha = Math.pow(size.toDouble(), alpha)

    fun words(): Stream<String> = Files.lines(Paths.get(filename))
            .map { it.split(Regex("\\s+")) }
            .flatMap { it.stream() }
            .map(this::transform)
            .filter(CharSequence::isNotEmpty)

    fun updateCoOccurences(stimulus: String, window: Window) {
        window.words(stimulus).forEach {
            val pair = Pair(stimulus, it!!)
            val occurences = coocurrences.getOrDefault(pair, 0)
            coocurrences[pair] = occurences + 1
        }
    }

    fun has(word: String) = occurences.containsKey(word)

    fun associationsFor(stimulus: String) = Pair(stimulus, occurences.keys
            .stream()
            .map { Pair(it, calculateStrength(stimulus, it)) }
            .asSequence()
            .sortedBy { it.second }
            .take(10)
            .toList()
    )

    private fun transform(word: String) = stem(word.toLowerCase().replace(Regex("[^a-ząćęłóńśżź]"), ""))

    private fun stem(word: String): String {
        val stem = stemmer.lookup(word)
        return if (stem.size > 0) stem[0].stem.toString() else word
    }

    private fun calculateStrength(stimulus: String, word: String) =
            sizeToAlpha / occurences[stimulus]!! *
                    coocurrences.getOrDefault(Pair(stimulus, word), 0) / weaken(word)

    private fun weaken(word: String): Double {
        val occurence = occurences[word]!!
        return if (occurence > betaSize) Math.pow(occurence.toDouble(), alpha) else gammaSize
    }
}
