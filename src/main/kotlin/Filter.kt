import java.io.File

fun valid(word: String) = word != "puste" && word.length > 3

fun clean(word: String) = word
        .trim()
        .toLowerCase()
        .replace(Regex("[^a-ząćęłóńśżź\\s]"), "")

fun process(file: File) {
    println(file.nameWithoutExtension)

    file.bufferedReader().lines().map {
        val (number, word) = it.split(",")
        Pair(number.toInt(), word)
    }
            .filter { it.first > 2 }
            .map { clean(it.second) }
            .filter { valid(it) }
            .limit(10)
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
