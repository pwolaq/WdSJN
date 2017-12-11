import java.util.*

class Window(val size: Int) {
    private val items: LinkedList<String?> = LinkedList()

    init {
        for (index in 0..size * 2) {
            items.add(null)
        }
    }

    fun currentWord() = items[size]

    fun sentence() = items.joinToString(" ")

    fun words(stimulus: String): List<String> = items.filter { it != stimulus }.requireNoNulls()

    fun slide(word: String?) {
        items.removeFirst()
        items.addLast(word)
    }
}