package ru.skillbranch.skillarticles.extensions

fun String?.indexesOf(query: String, ignoreCase: Boolean = true): List<Int> {
    if (query.isEmpty() || this == null) return emptyList()
    val indexes = mutableListOf<Int>()
    var startIndex = 0
    while (startIndex <= this.length - query.length) {
        val index = if (ignoreCase) this.toLowerCase().indexOf(query.toLowerCase(), startIndex)
        else this.indexOf(query, startIndex)
        if (index == -1) break
        indexes.add(index)
        startIndex = index + query.length
    }
    return indexes
}
