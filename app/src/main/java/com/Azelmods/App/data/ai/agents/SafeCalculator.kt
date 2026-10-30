package com.Azelmods.App.data.ai.agents

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Calculadora de la herramienta `calculator` de los agentes.
 *
 * Los modelos de lenguaje se equivocan con la aritmética; con esta herramienta
 * el agente delega las cuentas en lugar de "estimarlas". Es un analizador
 * descendente recursivo propio: NO evalúa código (nada de motores de script),
 * así que una entrada maliciosa solo puede producir un error de sintaxis.
 *
 * Admite + - * / ^, paréntesis, coma o punto decimal, porcentajes con % ("15%
 * de 80" se escribe 80*15%), constantes pi y e, y las funciones sqrt, abs,
 * round, floor, ln, log, sin, cos, tan (en radianes).
 */
object SafeCalculator {

    class CalculatorException(message: String) : IllegalArgumentException(message)

    fun evaluate(expression: String): Double {
        val normalized = expression
            .replace('×', '*').replace('÷', '/').replace('−', '-')
            .replace(Regex("(?<=\\d),(?=\\d)"), ".")
            .trim()
        if (normalized.isEmpty()) throw CalculatorException("Expresión vacía")
        if (normalized.length > 300) throw CalculatorException("Expresión demasiado larga")
        val parser = Parser(normalized)
        val value = parser.parseExpression()
        parser.skipSpaces()
        if (!parser.atEnd()) throw CalculatorException("Símbolo inesperado en la posición ${parser.pos + 1}")
        if (value.isNaN() || value.isInfinite()) throw CalculatorException("Resultado no definido")
        return value
    }

    /** Resultado legible: sin ".0" en enteros y como mucho 10 decimales. */
    fun format(value: Double): String {
        if (value == floor(value) && abs(value) < 1e15) return value.toLong().toString()
        return "%.10f".format(java.util.Locale.ROOT, value).trimEnd('0').trimEnd('.')
    }

    private class Parser(private val s: String) {
        var pos = 0

        fun atEnd() = pos >= s.length

        fun skipSpaces() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        private fun peek(): Char? {
            skipSpaces()
            return if (pos < s.length) s[pos] else null
        }

        private fun eat(c: Char): Boolean {
            if (peek() == c) {
                pos++
                return true
            }
            return false
        }

        // expression := term (('+'|'-') term)*
        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                value = when {
                    eat('+') -> value + parseTerm()
                    eat('-') -> value - parseTerm()
                    else -> return value
                }
            }
        }

        // term := factor (('*'|'/') factor)*
        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                value = when {
                    eat('*') -> value * parseFactor()
                    eat('/') -> {
                        val d = parseFactor()
                        if (d == 0.0) throw CalculatorException("División entre cero")
                        value / d
                    }
                    else -> return value
                }
            }
        }

        // factor := unary ('^' factor)?
        private fun parseFactor(): Double {
            val base = parseUnary()
            return if (eat('^')) base.pow(parseFactor()) else base
        }

        private fun parseUnary(): Double = when {
            eat('-') -> -parseUnary()
            eat('+') -> parseUnary()
            else -> parsePostfix()
        }

        // postfix := primary '%'?   (porcentaje: 15% = 0.15)
        private fun parsePostfix(): Double {
            val value = parsePrimary()
            return if (eat('%')) value / 100.0 else value
        }

        private fun parsePrimary(): Double {
            val c = peek() ?: throw CalculatorException("Falta un número al final")
            if (c == '(') {
                pos++
                val v = parseExpression()
                if (!eat(')')) throw CalculatorException("Falta cerrar un paréntesis")
                return v
            }
            if (c.isDigit() || c == '.') return parseNumber()
            if (c.isLetter()) return parseIdentifier()
            throw CalculatorException("Símbolo inesperado '$c'")
        }

        private fun parseNumber(): Double {
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            // Notación científica: 1.5e3
            if (pos < s.length && (s[pos] == 'e' || s[pos] == 'E') &&
                pos + 1 < s.length && (s[pos + 1].isDigit() || s[pos + 1] == '-' || s[pos + 1] == '+')
            ) {
                pos++
                if (s[pos] == '-' || s[pos] == '+') pos++
                while (pos < s.length && s[pos].isDigit()) pos++
            }
            return s.substring(start, pos).toDoubleOrNull()
                ?: throw CalculatorException("Número no válido: ${s.substring(start, pos)}")
        }

        private fun parseIdentifier(): Double {
            val start = pos
            while (pos < s.length && s[pos].isLetter()) pos++
            val name = s.substring(start, pos).lowercase()
            when (name) {
                "pi" -> return Math.PI
                "e" -> return Math.E
            }
            if (!eat('(')) throw CalculatorException("Se esperaba '(' después de $name")
            val arg = parseExpression()
            if (!eat(')')) throw CalculatorException("Falta cerrar el paréntesis de $name")
            return when (name) {
                "sqrt", "raiz" -> {
                    if (arg < 0) throw CalculatorException("Raíz de un número negativo")
                    sqrt(arg)
                }
                "abs" -> abs(arg)
                "round", "redondear" -> round(arg)
                "floor" -> floor(arg)
                "ln" -> ln(arg)
                "log" -> log10(arg)
                "sin", "sen" -> sin(arg)
                "cos" -> cos(arg)
                "tan" -> tan(arg)
                else -> throw CalculatorException("Función desconocida: $name")
            }
        }
    }
}
