package schedule
import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed interface ParseResult {
    data class Success(val session: Session) : ParseResult
    data class Failure(val lineNumber: Long, val reason: ParseError) : ParseResult
}

sealed interface ParseError {
    object InvalidSeparator : ParseError
    data class WrongFieldCount(val count: Int) : ParseError
    data class EmptyField(val fieldName: String, val value: String) : ParseError
    data class InvalidDate(val value: String) : ParseError
    data class UnknownType(val value: String) : ParseError
    data class ExamTeachersRequired(val got: Int) : ParseError
    data class DomainViolation(val message: String) : ParseError
}

fun parseLine(number: Long, line: String): ParseResult {
    if (!line.contains('|')) return ParseResult.Failure(number, ParseError.InvalidSeparator)
    val parts = line.split('|', limit = 6)
    if (parts.size != 5) return ParseResult.Failure(number, ParseError.WrongFieldCount(parts.size))

    val (dateStr, typeStr, topic, location, teachersStr) = parts
    val fieldNames = listOf("date", "type", "topic", "location", "teachers")
    val values = listOf(dateStr, typeStr, topic, location, teachersStr)

    for (i in values.indices) {
        if (values[i].isBlank()) return ParseResult.Failure(number, ParseError.EmptyField(fieldNames[i], values[i]))
    }

    val date = try { LocalDate.parse(dateStr) } catch (_: DateTimeParseException) {
        return ParseResult.Failure(number, ParseError.InvalidDate(dateStr))
    }
    if (date.year !in 1..9999) return ParseResult.Failure(number, ParseError.InvalidDate(dateStr))

    val sessionType = when (typeStr) {
        "LECTURE" -> SessionType.LECTURE; "PRACTICE" -> SessionType.PRACTICE
        "EXAM" -> SessionType.EXAM; "CANCELLED" -> SessionType.CANCELLED
        else -> return ParseResult.Failure(number, ParseError.UnknownType(typeStr))
    }

    return try {
        val teachers = if (teachersStr == "-") emptyList() else teachersStr.split(",")
        val session = when (sessionType) {
            SessionType.LECTURE -> Lecture(date, topic, location, teachers)
            SessionType.PRACTICE -> Practice(date, topic, location, teachers)
            SessionType.EXAM -> {
                if (teachers.size < 2) return ParseResult.Failure(number, ParseError.ExamTeachersRequired(teachers.size))
                Exam(date, topic, location, teachers)
            }
            SessionType.CANCELLED -> {
                if (location != "-" || teachersStr != "-") 
                    return ParseResult.Failure(number, ParseError.DomainViolation("Cancelled session must have '-' for location and teachers"))
                Cancelled(date, topic)
            }
        }
        ParseResult.Success(session)
    } catch (e: IllegalArgumentException) {
        ParseResult.Failure(number, ParseError.DomainViolation(e.message ?: "Invalid data"))
    }
}

fun formatParseError(result: ParseResult.Failure): String {
    val n = result.lineNumber
    return when (val err = result.reason) {
        is ParseError.InvalidSeparator -> "line $n: invalid field separator"
        is ParseError.WrongFieldCount -> "line $n: got ${err.count} fields, expected 5"
        is ParseError.EmptyField -> "line $n: empty required field \"${escape(err.fieldName)}\""
        is ParseError.InvalidDate -> "line $n: invalid date \"${escape(err.value)}\""
        is ParseError.UnknownType -> "line $n: unknown session type \"${escape(err.value)}\""
        is ParseError.ExamTeachersRequired -> "line $n: type \"EXAM\" requires at least 2 teachers, got ${err.got}"
        is ParseError.DomainViolation -> "line $n: ${escape(err.message)}"
    }
}

private fun escape(s: String): String = s
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .map { c -> if (c.isISOControl() || c.code > 127) "\\u%04X".format(c.code) else c.toString() }
    .joinToString("")
