package org.jikvict.problems.util

object NameFormatter {
    private const val SLASH = '/'
    private const val DASH = '-'
    private val SEPARATOR_CONVERTING_CHARS = """\s|_""".toRegex()
    private val INVALID_CHARS = "[^a-zA-Z0-9.$SLASH$DASH]".toRegex()
    private val AFTER_NON_UPPER_OR_SEPARATOR = "(?<!\\p{Upper}|$SLASH|$DASH)".toRegex()
    private val BEFORE_UPPER = """(?=\p{Upper})""".toRegex()
    private val CAMEL_CASE_INSERT = "$AFTER_NON_UPPER_OR_SEPARATOR$BEFORE_UPPER".toRegex()
    private val METHOD_REFERENCE_INSERTER = "\\.-".toRegex()

    fun format(value: String) =
        value
            .replace(SEPARATOR_CONVERTING_CHARS, DASH.toString())
            .replace(INVALID_CHARS, "")
            .replace(CAMEL_CASE_INSERT, DASH.toString())
            .replace(METHOD_REFERENCE_INSERTER, SLASH.toString())
            .trim(SLASH, DASH)
            .lowercase()
}
