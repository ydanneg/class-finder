package com.ydanneg.classfinder

import java.io.File
import javax.lang.model.SourceVersion

data class ClassName(val pkg: String?, val name: String)

fun ClassName.qualifiedName(): String = "${pkg?.let { "$it." } ?: ""}$name"

data class SearchQuery(val start: String?, val end: String?)

data class SearchPattern(val searchWords: List<String>, val ignoreCase: Boolean = true, val matchEnding: Boolean = false)

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Usage: class-finder <filename> '<pattern>'")
        return
    }

    val (file, pattern) = args
    ClassFinder()
        .findClasses(File(file).readLines(), pattern)
        .forEach { println(it) }
}


class ClassFinder {

    fun findClasses(lines: List<String>, pattern: String): List<String> =
        lines
            .asSequence()
            .map { it.trim() }
            .filter { SourceVersion.isName(it) }
            .map { parseClassName(it) }
            .filter { it.name.isNotBlank() }
            .filter { match(it.name, pattern) }
            .distinct()
            .sortedBy { it.name }
            .map { it.qualifiedName() }
            .toList()
}

/**
 * Splits the input string by words.
 * Words are separated by uppercase letters.
 * If the input string is empty, an empty list will be returned.
 *
 * Example: "HelloWorld" -> ["Hello", "World"]
 */
fun splitByWords(input: String): List<String> {
    var previousIndex = 0
    val words = mutableListOf<String>()
    input.toCharArray().forEachIndexed { index, char ->
        if (char.isUpperCase() && previousIndex != index) {
            val word = input.substring(previousIndex, index)
            words.add(word)
            previousIndex = index
        }
    }
    if (input.length > previousIndex) {
        words.add(input.substring(previousIndex))
    }
    return words
}

/**
 * Parses the input string to a [ClassName] object.
 * The input string should be in the format of a fully qualified class name.
 * The package name is optional.
 * If the package name is not present, the [ClassName.pkg] will be null.
 * The [ClassName.name] will always be present.
 * The input string will be trimmed before parsing.
 * If the input string is empty, the [ClassName.name] will be empty.
 * If the input string is not a valid class name, the [ClassName.name] will be empty.
 * If the input string is not a valid class name, the [ClassName.pkg] will be null.
 * Example: "com.ydanneg.ClassFinder" -> ClassName("com.ydanneg", "ClassFinder")
 */
fun parseClassName(className: String): ClassName {
    val trimmed = className.trim()
    val lastIndexOfDot = trimmed.lastIndexOf(".")
    if (lastIndexOfDot == -1) {
        return ClassName(null, trimmed)
    }
    return ClassName(trimmed.substring(0, lastIndexOfDot), trimmed.substring(lastIndexOfDot + 1, trimmed.length))
}

fun findMatchedIndex(inputWords: List<String>, index: Int, searchWord: String, ignoreCase: Boolean = false): Int {
    val searchQuery = searchWord.asSearchQuery()
    if (index < inputWords.size) {
        for (i in index until inputWords.size) {
            if (searchQuery.start != null && !inputWords[i].startsWith(searchQuery.start, ignoreCase = ignoreCase)) {
                continue
            }
            if (searchQuery.end != null && !inputWords[i].endsWith(searchQuery.end, ignoreCase = ignoreCase)) {
                continue
            }
            return i
        }
    }
    return -1
}

fun match(inputString: String, pattern: String): Boolean {
    val input = splitByWords(inputString)
    val searchPattern = pattern.asSearchPattern()

    if (searchPattern.ignoreCase && inputString.contains(pattern, true)) {
        return true
    }

    if (searchPattern.searchWords.size > input.size) {
        return false
    }

    // reverse input and search words if matchEnding is true
    val inputWords = if (searchPattern.matchEnding) input.reversed() else input
    val searchWords = if (searchPattern.matchEnding) searchPattern.searchWords.reversed() else searchPattern.searchWords

    var offset = 0
    var matched = false
    // try matching with offset if input string size allows it
    while (!matched && inputWords.size - offset >= searchWords.size) {
        var index = offset
        for (searchWord in searchWords) {
            val matchedIndex = findMatchedIndex(inputWords, index, searchWord, searchPattern.ignoreCase)
            if (matchedIndex == -1 || ((index != 0 || searchPattern.matchEnding) && matchedIndex > index)) {
                matched = false
                if (searchPattern.matchEnding) {
                    return false
                }
                break
            }
            matched = true
            index = matchedIndex + 1
        }
        offset++
    }
    return matched
}

fun String.asSearchPattern(): SearchPattern {
    val hasTrailingSpace = this.endsWith(" ")
    val trimmed = this.trim()
    var ignoreCase = false
    return trimmed.let {
        if (trimmed.all { ch -> ch.isLowerCase() }) {
            ignoreCase = true
            it.uppercase()
        } else {
            it
        }
    }.let {
        SearchPattern(
            searchWords = splitByWords(it),
            ignoreCase = ignoreCase,
            matchEnding = hasTrailingSpace
        )
    }
}

fun String.asSearchQuery(): SearchQuery {
    var previousIsAsterisk = false
    val normalized = buildString {
        this@asSearchQuery.forEach {
            if (it == '*') {
                if (!previousIsAsterisk) {
                    append(it)
                    previousIsAsterisk = true
                }
            } else {
                previousIsAsterisk = false
                append(it)
            }
        }

    }
    val lastIndexOf = normalized.lastIndexOf("*")
    val startsWith = if (lastIndexOf != -1) normalized.substring(0, lastIndexOf) else normalized
    val endsWith = if (lastIndexOf != -1) normalized.substring(lastIndexOf + 1) else null
    return SearchQuery(startsWith.ifEmpty { null }, endsWith?.ifEmpty { null })
}
