package ru.skillbranch.skillarticles.extensions

fun String?.indexesOf(
    substr: String,
    ignoreCase: Boolean = true
): List<Int> {
    this ?: return emptyList()
    if (substr.isEmpty()) return emptyList()

    val index = this.indexOf(substr, ignoreCase = ignoreCase)
    return if (index != -1) {
        listOf(index).plus(
            this.substring(index + substr.length)
                .indexesOf(substr, ignoreCase)
                .map { it + index + substr.length }
        )
    } else {
        emptyList()
    }
}