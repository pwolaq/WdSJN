import morfologik.stemming.polish.PolishStemmer
import java.io.File

val regex = Regex("[^a-ząćęłóńśżź\\s]")
val stemmer = PolishStemmer()

fun stem(word: String): String {
    val stem = stemmer.lookup(word)
    return if (stem.size > 0) stem[0].stem.toString() else word
}

fun valid(word: String) = word != "puste" && word.length > 3

fun clean(word: String) = word
        .trim()
        .toLowerCase()
        .replace(regex, "")

fun process(file: File) {
    println(file.nameWithoutExtension)

    file.bufferedReader().lines().map {
        val (number, word) = it.split(",")
        Pair(number.toInt(), word)
    }
            .filter { it.first > 2 }
            .map { clean(it.second) }
            .filter { valid(it) }
            .map { stem(it) }
            .distinct()
            .limit(30)
            .forEach { println("\t$it") }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        throw IllegalArgumentException("Invalid number of arguments. At least one stimulus file is required.")
    }

    args.forEach {
        process(File(it))
    }
}
