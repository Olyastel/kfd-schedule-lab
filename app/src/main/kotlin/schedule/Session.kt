package schedule
import java.time.LocalDate
import java.util.Collections

sealed interface Session {
    val date: LocalDate
    val topic: String
}

private fun validateString(value: String, fieldName: String) {
    if (value.isBlank()) throw IllegalArgumentException("Field '$fieldName' must not be blank")
    if (value.first().isWhitespace() || value.last().isWhitespace() ||
        value.any { it.isISOControl() && !it.isWhitespace() }) {
        throw IllegalArgumentException("Field '$fieldName' contains invalid characters")
    }
}

private fun validateTeachers(names: List<String>) {
    if (names.isEmpty()) throw IllegalArgumentException("Teachers list must not be empty")
    names.forEachIndexed { index, name ->
        if (name.isBlank() || name == "-") throw IllegalArgumentException("Teacher at index $index is invalid ('$name')")
        validateString(name, "teacher[$index]")
    }
    if (names.distinct().size != names.size) throw IllegalArgumentException("Duplicate teachers found")
}

class Lecture(
    override val date: LocalDate,
    override val topic: String,
    val location: String,
    teachers: List<String>
) : Session {
    // UnmodifiableList гарантирует, что приведение к MutableList вернет null
    // или выбросит UnsupportedOperationException при попытке изменения
    val teachers: List<String> = Collections.unmodifiableList(teachers.toList())

    init {
        require(date.year in 1..9999) { "Year must be 1..9999" }
        validateString(topic, "topic"); validateString(location, "location")
        require(location != "-") { "Location cannot be '-' for active session" }
        validateTeachers(this.teachers)
    }
}

class Practice(
    override val date: LocalDate,
    override val topic: String,
    val location: String,
    teachers: List<String>
) : Session {
    val teachers: List<String> = Collections.unmodifiableList(teachers.toList())

    init {
        require(date.year in 1..9999) { "Year must be 1..9999" }
        validateString(topic, "topic"); validateString(location, "location")
        require(location != "-") { "Location cannot be '-' for active session" }
        validateTeachers(this.teachers)
    }
}

class Exam(
    override val date: LocalDate,
    override val topic: String,
    val location: String,
    teachers: List<String>
) : Session {
    val teachers: List<String> = Collections.unmodifiableList(teachers.toList())

    init {
        require(date.year in 1..9999) { "Year must be 1..9999" }
        validateString(topic, "topic"); validateString(location, "location")
        require(location != "-") { "Location cannot be '-' for active session" }
        require(this.teachers.size >= 2) { "Exam requires at least 2 teachers, got ${this.teachers.size}" }
        validateTeachers(this.teachers)
    }
}

class Cancelled(
    override val date: LocalDate,
    override val topic: String
) : Session {
    init {
        require(date.year in 1..9999) { "Year must be 1..9999" }
        validateString(topic, "topic")
    }
}