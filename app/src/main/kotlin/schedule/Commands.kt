package schedule

interface CommandHandlers {
    fun period(command: Command.Period): Int
    fun byType(command: Command.ByType): Int
    fun errors(command: Command.Errors): Int
}

object StudentCommandHandlers : CommandHandlers {
    override fun period(command: Command.Period): Int = runPeriod(command)
    override fun byType(command: Command.ByType): Int = runType(command)
    override fun errors(command: Command.Errors): Int = runErrors(command)
}

fun runPeriod(command: Command.Period): Int {
    var total = 0
    var hasErrors = false

    forEachScheduleLine(command.file) { number, line ->
        when (val result = parseLine(number, line)) {
            is ParseResult.Success -> {
                val s = result.session
                if (s.date in command.from..command.to) {
                    println("${s.date} | ${getTypeLabel(s)} | ${s.topic} | ${getLocation(s)}")
                    total++
                }
            }
            is ParseResult.Failure -> hasErrors = true
        }
    }

    println("Total: $total")
    return if (hasErrors) 1 else 0
}

fun runType(command: Command.ByType): Int {
    var total = 0
    var hasErrors = false

    forEachScheduleLine(command.file) { number, line ->
        when (val result = parseLine(number, line)) {
            is ParseResult.Success -> {
                val s = result.session
                if (matchesType(s, command.type)) {
                    val teachersStr = if (s is Cancelled) "-" else getTeachers(s).joinToString(", ")
                    println("${s.date} | ${getTypeLabel(s)} | ${s.topic} | ${getLocation(s)} | $teachersStr")
                    total++
                }
            }
            is ParseResult.Failure -> hasErrors = true
        }
    }

    println("Total: $total")
    return if (hasErrors) 1 else 0
}

fun runErrors(command: Command.Errors): Int {
    var rejected = 0L
    var totalLines = 0L

    forEachScheduleLine(command.file) { number, line ->
        totalLines++
        when (val result = parseLine(number, line)) {
            is ParseResult.Failure -> {
                println(formatParseError(result))
                rejected++
            }
            is ParseResult.Success -> {}
        }
    }

    println("Rejected: $rejected of $totalLines")
    return if (rejected > 0) 1 else 0
}

private fun getTypeLabel(s: Session): String = when (s) {
    is Lecture -> "LECTURE"; is Practice -> "PRACTICE"
    is Exam -> "EXAM"; is Cancelled -> "CANCELLED"
}

private fun getLocation(s: Session): String = when (s) {
    is Cancelled -> "-"
    is Lecture -> s.location; is Practice -> s.location; is Exam -> s.location
}

private fun getTeachers(s: Session): List<String> = when (s) {
    is Cancelled -> emptyList()
    is Lecture -> s.teachers; is Practice -> s.teachers; is Exam -> s.teachers
}

private fun matchesType(s: Session, type: SessionType): Boolean = when (type) {
    SessionType.LECTURE -> s is Lecture; SessionType.PRACTICE -> s is Practice
    SessionType.EXAM -> s is Exam; SessionType.CANCELLED -> s is Cancelled
}
