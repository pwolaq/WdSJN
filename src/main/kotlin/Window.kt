class Window(val size: Int) {
    private val items: MutableList<String?> = mutableListOf()

    init {
        for (index in 0..size * 2) {
            items.add(null)
        }
    }

    fun currentWord() = items[size]

    fun words(stimulus: String) = items.filter { it != null && it != stimulus }

    fun slide(word: String?) {
        items.removeAt(0)
        items.add(word)
    }
}