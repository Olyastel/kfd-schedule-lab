package schedule

interface SessionRepository {
    fun add(session: Session)
    fun addAll(sessions: List<Session>)
    fun all(): List<Session>
}

class InMemorySessionRepository : SessionRepository {
    private val sessions = mutableListOf<Session>()

    override fun add(session: Session) {
        sessions.add(session)
    }

    override fun addAll(sessions: List<Session>) {
        this.sessions.addAll(sessions)
    }

    override fun all(): List<Session> = sessions.toList() // Возвращаем снимок
}

data class AuditEvent(val operation: String, val count: Int)

class AuditedSessionRepository(
    private val delegate: SessionRepository = InMemorySessionRepository()
) : SessionRepository by delegate {
    private val auditLog = mutableListOf<AuditEvent>()
    var totalAdded: Int = 0
        private set

    override fun add(session: Session) {
        delegate.add(session)
        auditLog.add(AuditEvent("add", 1))
        totalAdded += 1
    }

    override fun addAll(sessions: List<Session>) {
        delegate.addAll(sessions)
        auditLog.add(AuditEvent("addAll", sessions.size))
        totalAdded += sessions.size
    }

    fun getAuditLog(): List<AuditEvent> = auditLog.toList() // Снимок журнала
}