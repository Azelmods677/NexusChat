package com.Azelmods.App.data.chat

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.random.Random

/**
 * Comandos que se escriben en el chat: el resultado se envía como un mensaje normal, así que
 * lo ven todos los de la conversación. Se ejecutan en el teléfono de quien los escribe.
 */
object ChatCommands {

    data class Info(val name: String, val usage: String, val description: String)

    val all: List<Info> = listOf(
        Info("/descargar", "/descargar https://…", "Descarga un vídeo"),
        Info("/calc", "/calc 5+7*2", "Calculadora"),
        Info("/dado", "/dado", "Lanza un dado"),
        Info("/moneda", "/moneda", "Cara o cruz"),
        Info("/ppt", "/ppt piedra", "Piedra, papel o tijera"),
        Info("/hora", "/hora", "Hora actual"),
        Info("/fecha", "/fecha", "Fecha de hoy")
    )

    /** Sugerencias mientras se escribe el nombre del comando. */
    fun suggestions(typed: String): List<Info> {
        if (!typed.startsWith("/") || typed.contains(' ')) return emptyList()
        return all.filter { it.name.startsWith(typed, ignoreCase = true) }
    }

    /** Texto que se enviará, o null si [input] no es un comando y debe enviarse tal cual. */
    fun run(input: String, random: Random = Random.Default): String? {
        val trimmed = input.trim()
        if (!trimmed.startsWith("/")) return null
        val name = trimmed.substringBefore(' ').lowercase(Locale.ROOT)
        val argument = trimmed.substringAfter(' ', "").trim()
        return when (name) {
            "/calc" -> runCatching { "$argument = ${formatNumber(Calculator.evaluate(argument))}" }.getOrNull()
            "/dado" -> "Dado: ${random.nextInt(1, 7)}"
            "/moneda" -> if (random.nextBoolean()) "Moneda: cara" else "Moneda: cruz"
            "/ppt" -> rockPaperScissors(argument, random)
            "/hora" -> "Hora: ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())}"
            "/fecha" -> "Fecha: ${DateFormat.getDateInstance(DateFormat.LONG).format(Date())}"
            else -> null
        }
    }

    private fun rockPaperScissors(choice: String, random: Random): String? {
        val options = listOf("piedra", "papel", "tijera")
        val mine = options.indexOf(choice.lowercase(Locale.ROOT))
        if (mine < 0) return null
        val other = random.nextInt(options.size)
        val outcome = when ((mine - other + options.size) % options.size) {
            0 -> "Empate"
            1 -> "Ganas"
            else -> "Pierdes"
        }
        return "${options[mine]} contra ${options[other]}: $outcome"
    }

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble() && kotlin.math.abs(value) < 1e15) {
            value.toLong().toString()
        } else {
            String.format(Locale.ROOT, "%.6f", value).trimEnd('0').trimEnd('.')
        }

    /**
     * Calculadora propia, sin evaluar código: suma, resta, producto, división, resto, potencia,
     * paréntesis y decimales. Con límites para que una expresión enorme no bloquee la app.
     */
    private class Calculator(private val text: String) {
        private var position = 0
        private var depth = 0

        companion object {
            private const val MAX_LENGTH = 200
            private const val MAX_DEPTH = 32

            fun evaluate(expression: String): Double {
                val normalized = expression.replace(" ", "").replace(',', '.')
                    .replace('x', '*').replace('×', '*').replace('÷', '/')
                require(normalized.isNotEmpty() && normalized.length <= MAX_LENGTH)
                val calculator = Calculator(normalized)
                val result = calculator.parseSum()
                require(calculator.position == normalized.length)
                require(result.isFinite())
                return result
            }
        }

        private fun parseSum(): Double {
            var value = parseProduct()
            while (position < text.length) {
                when (text[position]) {
                    '+' -> { position++; value += parseProduct() }
                    '-' -> { position++; value -= parseProduct() }
                    else -> return value
                }
            }
            return value
        }

        private fun parseProduct(): Double {
            var value = parsePower()
            while (position < text.length) {
                when (text[position]) {
                    '*' -> { position++; value *= parsePower() }
                    '/' -> { position++; value /= parsePower() }
                    '%' -> { position++; value %= parsePower() }
                    else -> return value
                }
            }
            return value
        }

        private fun parsePower(): Double {
            val base = parseUnary()
            if (position < text.length && text[position] == '^') {
                position++
                return base.pow(parsePower())
            }
            return base
        }

        private fun parseUnary(): Double {
            require(++depth <= MAX_DEPTH)
            val value = when {
                position < text.length && text[position] == '-' -> { position++; -parseUnary() }
                position < text.length && text[position] == '+' -> { position++; parseUnary() }
                position < text.length && text[position] == '(' -> {
                    position++
                    val inner = parseSum()
                    require(position < text.length && text[position] == ')')
                    position++
                    inner
                }
                else -> parseNumber()
            }
            depth--
            return value
        }

        private fun parseNumber(): Double {
            val start = position
            while (position < text.length && (text[position].isDigit() || text[position] == '.')) position++
            require(position > start)
            return text.substring(start, position).toDouble()
        }
    }
}
