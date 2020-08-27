package calculator

import java.lang.Exception
import java.lang.NumberFormatException
import java.math.BigInteger
import java.util.Scanner

val priority = mapOf(
        "(" to 1,
        ")" to 1,
        "*" to 2,
        "/" to 2,
        "+" to 3,
        "-" to 3)

const val errorExpression = "Invalid expression"
const val errorVariable = "Unknown variable"

fun replacePlusMinus(operations: String): String {
    val minusesCount = operations.filter { it == '-' }.length % 2
    return if (minusesCount > 0) "-" else "+"
}


fun getValue(variables: MutableMap<String, BigInteger>, line: String): String {
    return if (line in variables.keys) variables[line]!!.toString() else errorVariable
}

fun createRefreshVariable(variables: MutableMap<String, BigInteger>, line: String): String? {
    val splitLine = line.split("=").map { it.trim() }
    if (splitLine.size > 2) return "Invalid assignment"
    val name = splitLine[0]
    if (name.contains("[^a-zA-z]".toRegex())) return "Invalid identifier"
    try {
        val value = splitLine[1].toBigInteger()
        variables[name] = value
    } catch (e: NumberFormatException) {
        val oldValueName = splitLine[1]
        if (oldValueName.contains("[^a-zA-z]".toRegex())) return "Invalid assignment"
        if (oldValueName in variables.keys) variables[name] = variables[oldValueName]!!
        else return errorVariable
    }
    return null
}

fun prepareExpressionLine(line: String): List<String> {
    var newLine = line
    while ("[+-]{2,}+".toRegex().containsMatchIn(newLine)) {
        val op = "[+-]{2,}+".toRegex().find(newLine)!!
        newLine = newLine.replace(op.groupValues[0], replacePlusMinus(op.groupValues[0]))
    }
    return "(\\w+|[+*/()-])".toRegex().findAll(newLine).map { it.groupValues[1] }.toList()
}

fun infixToPostfix(variables: MutableMap<String, BigInteger>, line: String): String {
    if (line.count { it -> it == '(' } != line.count { it -> it == ')' }) return errorExpression
    val splitExpression = prepareExpressionLine(line)
    val operators = mutableListOf<String>()
    var postfixExpression = ""

    var c = 0
    while (c < splitExpression.size) {
        when {
            splitExpression[c].toBigIntegerOrNull() != null -> postfixExpression += " " + splitExpression[c]
            "[a-zA-Z]+".toRegex().containsMatchIn(splitExpression[c]) -> {
                if (splitExpression[c] !in variables.keys) return errorVariable
                postfixExpression += " " + variables[splitExpression[c]]
            }
            splitExpression[c] == "(" -> operators.add(splitExpression[c])
            splitExpression[c] == ")" -> {
                var j = operators.lastIndex
                while (operators[j] != "(") {
                    postfixExpression += " " + operators[j]
                    operators.removeAt(operators.lastIndex)
                    --j
                }
                operators.removeAt(operators.lastIndex)
            }
            splitExpression[c] in "*/" -> {
                if (splitExpression[c + 1] == "*/") return errorExpression
                operators.add(splitExpression[c])
            }
            else -> {
                var j = operators.lastIndex
                while (j >= 0 && priority[splitExpression[c]]!! >= priority[operators[j]]!! && operators[j] !in "()") {
                    postfixExpression += " " + operators[j]
                    operators.removeAt(operators.lastIndex)
                    --j
                }
                operators.add(splitExpression[c])
            }
        }
        ++c
    }
    return (postfixExpression + " " + operators.asReversed().joinToString(" ")).trim()
}

fun evaluateExpression(variables: MutableMap<String, BigInteger>, line: String): String {
    try {
        val expression = infixToPostfix(variables, line)
        val result = mutableListOf<BigInteger>()
        for (element in expression.split(" ")) {
            when {
                element.toBigIntegerOrNull() != null -> result.add(element.toBigInteger())
                element == "+" -> {result.add(result.removeAt(result.lastIndex) + result.removeAt(result.lastIndex))}
                element == "-" -> {result.add(result.removeAt(result.lastIndex - 1) - result.removeAt(result.lastIndex))}
                element == "*" -> {result.add(result.removeAt(result.lastIndex) * result.removeAt(result.lastIndex))}
                element == "/" -> {result.add(result.removeAt(result.lastIndex - 1) / result.removeAt(result.lastIndex))}
            }
        }
        return result.first().toString()
    } catch (e: Exception) {
        return errorExpression
    }
}

fun main() {
    val scan = Scanner(System.`in`)
    val variables = mutableMapOf<String, BigInteger>()
    whenMark@ while (scan.hasNextLine()) {
        val line = scan.nextLine()
        when {
            line == "/exit" -> break@whenMark
            line == "/help" -> println("Input correct expression with operators: +, -, *, /, (, )")
            "/\\w+".toRegex().matches(line) -> println("Unknown command")
            line.toIntOrNull() is Int -> println(line)
            line.contains("[+*/-]".toRegex()) -> println(evaluateExpression(variables, line))
            line.contains("[=]".toRegex()) -> {
                val respVar = createRefreshVariable(variables, line)
                if (respVar != null) println(respVar)
            }
            !line.contains("/") && line.isNotEmpty() -> println(getValue(variables, line))
            line.isEmpty() -> continue@whenMark
        }
    }
    println("Bye!")
}
