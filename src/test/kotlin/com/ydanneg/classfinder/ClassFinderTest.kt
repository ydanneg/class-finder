package com.ydanneg.classfinder

import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClassFinderTest {

    @Test
    fun `should parse classname and restore qualified name`() {
        assertParsedQualifiedName("a.b.FooBarBaz", "a.b.FooBarBaz")
        assertParsedQualifiedName("codeborne.MindReader", "codeborne.MindReader")
        assertParsedQualifiedName("FooBarBaz", "FooBarBaz")
        assertParsedQualifiedName("a.b.", "a.b.")
        assertParsedQualifiedName(".FooBarBaz", ".FooBarBaz")
        assertParsedQualifiedName("     FooBarBaz     ", "FooBarBaz")
    }

    private fun assertParsedQualifiedName(input: String, expected: String) {
        assertEquals(expected, parseClassName(input).qualifiedName())
    }

    @Test
    fun `should split string by words`() {
        assertSplitByWords("FooBarBaz", listOf("Foo", "Bar", "Baz"))
        assertSplitByWords("fooBarBaz", listOf("foo", "Bar", "Baz"))
        assertSplitByWords("FBB", listOf("F", "B", "B"))
        assertSplitByWords("", emptyList())
    }

    private fun assertSplitByWords(input: String, expected: List<String>) {
        assertEquals(expected, splitByWords(input))
    }

    @Test
    fun `should find matched index`() {
        assertMatchedIndex(listOf("Foo", "Bar", "Baz"), 0, "Bar", 1)
        assertMatchedIndex(listOf("Foo", "Bar", "Baz"), 0, "Baz", 2)

        assertMatchedIndex(listOf("Foo", "Bar", "Baz"), 1, "Bar", 1)
        assertMatchedIndex(listOf("Foo", "Bar", "Baz"), 1, "Baz", 2)

        assertMatchedIndex(listOf("Foo", "Bar", "Baz"), 0, "B*r", 1)
        assertMatchedIndex(listOf("Foo", "Bar", "Baz"), 0, "B*", 1)
        assertMatchedIndex(listOf("Foo", "Bar", "Baz"), 0, "*r", 1)
    }

    @Test
    fun `should NOT find matched index`() {
        assertMatchedIndex(listOf("Foo", "Bar", "Baz"), 1, "Foo", -1)
        assertMatchedIndex(listOf("Foo", "Bar", "Baz"), 3, "Foo", -1)
    }

    private fun assertMatchedIndex(input: List<String>, index: Int, searchWord: String, excepted: Int) {
        assertEquals(excepted, findMatchedIndex(input, index, searchWord))
    }

    private fun assertMatched(input: String, pattern: String) {
        assertTrue(match(input, pattern))
    }

    private fun assertNotMatched(input: String, pattern: String) {
        assertFalse(match(input, pattern))
    }

    @Test
    fun `should match`() {
        assertMatched("YoureLeavingUsHere", "leav")
        assertMatched("FooBarBaz", "FoBa")
        assertMatched("FooBarBaz", "BaBa")
        assertMatched("FooBarBaz", "BarBaz")
        assertMatched("FooBarBaz", "FB")
        assertMatched("FooBarBaz", "fbb")
        assertMatched("FooBarBaz", "*Baz")
        assertMatched("FooBarBaz", "B*rBaz")
        assertMatched("FooBarBaz", "B*Baz")
        assertMatched("FooBar", "FB")
        assertMatched("FooBar", "F*oBa")
        assertMatched("FooBar", "*oBa")
        assertMatched("FooBar", "*Ba")
        assertMatched("FooBarBaz", "FBar")
        assertMatched("FooBar", "FBar ")
        assertMatched("FooBarzoo", "FBar ")
    }

    @Test
    fun `should NOT match`() {
        assertNotMatched("FooBar", "FBB")
        assertNotMatched("FooBar", "BB")
        assertNotMatched("FooBar", "Bo")
        assertNotMatched("FooBarBaz", "BrBaz")
        assertNotMatched("FooBarBaz", "FBar ")
        assertNotMatched("FooBarBaz", "fBb")
    }

}