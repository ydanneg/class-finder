package com.ydanneg.classfinder

import java.io.File
import javax.lang.model.SourceVersion

data class ClassName(val pkg: String?, val name: String)

fun ClassName.qualifiedName(): String = "${pkg?.let { "$it." } ?: ""}$name"

data class SearchQuery(val start: String?, val end: String? = null)

data class SearchPattern(val searchWords: List<String>, val caseSensitive: Boolean = true, val matchEnding: Boolean = false)

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Usage: class-finder <filename> '<pattern>'")
        return
    }

    val (file, pattern) = args
    ClassFinder()
        .findClasses(file, pattern)
        .forEach { println(it) }
}


class ClassFinder {
    fun findClasses(file: String, pattern: String): List<String> {
        return File(file)
            .readLines()
            .asSequence()
            .map { it.trim() }
            .filter { SourceVersion.isName(it) }
            .map { parseClassName(it) }
            .filter { it.name.isNotBlank() }
            .filter { match(it.name, pattern) }
            .sortedBy { it.name }
            .map { it.qualifiedName() }
            .toList()
    }
}

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

fun parseClassName(input: String): ClassName {
    val trimmedInput = input.trim()
    val lastIndexOfDot = trimmedInput.lastIndexOf(".")
    if (lastIndexOfDot == -1) {
        return ClassName(null, trimmedInput)
    }
    return ClassName(trimmedInput.substring(0, lastIndexOfDot), trimmedInput.substring(lastIndexOfDot + 1, trimmedInput.length))
}

fun findMatchedIndex(input: List<String>, index: Int, searchWord: String, caseSensitive: Boolean = true): Int {
    val searchQuery = searchWord.asSearchQuery()
    if (index < input.size) {
        for (i in index until input.size) {
            if (searchQuery.start != null && !input[i].startsWith(searchQuery.start, ignoreCase = !caseSensitive)) {
                continue
            }
            if (searchQuery.end != null && !input[i].endsWith(searchQuery.end, ignoreCase = !caseSensitive)) {
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
    if (searchPattern.searchWords.size > input.size) {
        return false
    }

    // reverse input and search words if matchEnding is true
    val inputWords = if (searchPattern.matchEnding) input.reversed() else input
    val searchWords = if (searchPattern.matchEnding) searchPattern.searchWords.reversed() else searchPattern.searchWords
    val searchWordIterator = searchWords.iterator()

    var index = 0
    while (searchWordIterator.hasNext()) {
        val searchWord = searchWordIterator.next()
        val matchedIndex = findMatchedIndex(inputWords, index, searchWord, searchPattern.caseSensitive)
        // try next if first is not matched or fail if matchEnding is true
        if (matchedIndex == -1 || ((index != 0 || searchPattern.matchEnding) && matchedIndex > index)) {
            return false
        }
        index = matchedIndex + 1
    }
    return true
}

fun String.asSearchPattern(): SearchPattern {
    val endWithSpace = this.endsWith(" ")
    var caseSensitive = true
    return this.let {
        if (this.all { ch -> ch.isLowerCase() }) {
            caseSensitive = false
            it
        } else {
            it
        }
    }.let {
        SearchPattern(
            searchWords = splitByWords(it.trim()),
            caseSensitive = caseSensitive,
            matchEnding = endWithSpace
        )
    }
}

fun String.asSearchQuery(): SearchQuery {
    // TODO: replace "*+" with *
    val lastIndexOf = this.lastIndexOf("*")
    val startsWith = if (lastIndexOf != -1) this.substring(0, lastIndexOf) else this.ifEmpty { null }
    val endsWith = if (lastIndexOf != -1) this.substring(lastIndexOf + 1).ifEmpty { null } else null
    return SearchQuery(startsWith, endsWith)
}
