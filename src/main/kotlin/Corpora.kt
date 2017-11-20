import morfologik.stemming.polish.PolishStemmer
import mu.KLogging
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asSequence

class Corpora(private val filename: String) {
    private val alpha = 0.66
    private val beta = 0.00002
    private val gamma = 0.00002
    private val numberOfAssociations = 10

    private val dumpFilename = "$filename.dump"
    private val filterRegex = Regex("[^a-ząćęłóńśżź]")
    private val splitRegex = Regex("\\s+")
    private val stemmer = PolishStemmer()
    private val cooccurrences: MutableMap<Pair<String, String>, Int> = mutableMapOf()
    private val occurrences: Map<String, Int> by lazy(LazyThreadSafetyMode.NONE) {
        logger.debug("Counting occurrences...")
        val result = loadDump() ?: countOccurrences()
        logger.debug("Counting occurrences [DONE]")
        result
    }
    private val size: Int by lazy(LazyThreadSafetyMode.NONE) {
        logger.debug("Counting words...")
        val result = occurrences.values.sum()
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
            .map { it.split(splitRegex) }
            .flatMap { it.stream() }
            .map(this::transform)
            .filter(CharSequence::isNotEmpty)

    fun updateCoOccurences(stimulus: String, words: List<String?>) = words.forEach {
            val pair = Pair(stimulus, it!!)
            val occurrences = cooccurrences.getOrDefault(pair, 0)
            cooccurrences[pair] = occurrences + 1
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

    fun saveDump() {
        val file = File(dumpFilename)

        if (!file.exists()) {
            logger.debug("Saving dump to file: $dumpFilename...")
            file.createNewFile()
            file.setWritable(true)
            val writer = file.bufferedWriter()
            occurrences.entries.forEach {
                writer.write("${it.key},${it.value}")
                writer.newLine()
            }
            writer.flush()
            writer.close()
            logger.debug("Saving dump to file: $dumpFilename... [DONE]")
        }
    }

    private fun loadDump(): Map<String, Int>? {
        val file = File(dumpFilename)

        if (file.exists()) {
            logger.debug("Loading occurrences from dump file: $dumpFilename...")
            val map = HashMap<String, Int>()
            file.useLines {
                lines -> lines.forEach {
                    val (word, count) = it.split(",")
                    map[word] = count.toInt()
                }
            }
            logger.debug("Loading occurrences from dump file: $dumpFilename... [DONE]")
            return map
        }

        logger.info("Dump file not found.")

        return null
    }

    private fun countOccurrences(): Map<String, Int> {
        logger.debug("Counting occurrences manually...")
        val result = words().asSequence().groupingBy { it }.eachCount()
        logger.debug("Counting occurrences manually... [DONE]")
        return result
    }

    private fun transform(word: String) = stem(word.toLowerCase().replace(filterRegex, ""))

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
