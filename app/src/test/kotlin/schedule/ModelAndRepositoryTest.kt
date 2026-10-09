package schedule

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ModelAndRepositoryTest {

    @Test
    fun lectureRequiresAtLeastOneTeacher() {
        assertFailsWith<IllegalArgumentException> {
            Lecture(LocalDate.of(2026, 10, 5), "Topic", "Room", emptyList())
        }
    }

    @Test
    fun examRequiresAtLeastTwoTeachers() {
        assertFailsWith<IllegalArgumentException> {
            Exam(LocalDate.of(2026, 10, 15), "Exam", "Room", listOf("Ivanov"))
        }
    }

    @Test
    fun examRejectsDuplicateTeachers() {
        assertFailsWith<IllegalArgumentException> {
            Exam(LocalDate.of(2026, 10, 15), "Exam", "Room", listOf("Ivanov", "Ivanov"))
        }
    }

    @Test
    fun cancelledHasOnlyDateAndTopic() {
        val c = Cancelled(LocalDate.of(2026, 10, 19), "Postponed")
        assertEquals(LocalDate.of(2026, 10, 19), c.date)
        assertEquals("Postponed", c.topic)
    }

    @Test
    fun collectionProtectionExternalListMutationDoesNotAffectSession() {
        val names = mutableListOf("Ivanov", "Petrov")
        val lecture = Lecture(LocalDate.of(2026, 10, 5), "Topic", "Room", names)
        names.clear()
        assertEquals(2, lecture.teachers.size)
        assertEquals("Ivanov", lecture.teachers[0])
        assertEquals("Petrov", lecture.teachers[1])
    }

    @Test
    fun collectionProtectionReturnedListCannotModifySession() {
        val lecture = Lecture(LocalDate.of(2026, 10, 5), "Topic", "Room", listOf("Ivanov"))
        // Проверяем поведение: попытка изменить список должна выбросить UnsupportedOperationException
        assertFailsWith<UnsupportedOperationException> {
            (lecture.teachers as MutableList<String>).add("Hacker")
        }
    }

    @Test
    fun auditedRepositoryLogsAddAndAddAllCorrectly() {
        val repo = AuditedSessionRepository()
        val s1 = Lecture(LocalDate.of(2026, 10, 5), "L1", "R1", listOf("A"))
        val s2 = Practice(LocalDate.of(2026, 10, 8), "P1", "R1", listOf("B"))
        val s3 = Practice(LocalDate.of(2026, 10, 9), "P1", "R1", listOf("B"))

        repo.add(s1)
        repo.addAll(listOf(s2, s3))

        assertEquals(3, repo.totalAdded)
        assertEquals(2, repo.getAuditLog().size)
        assertEquals(1, repo.getAuditLog()[0].count)
        assertEquals(2, repo.getAuditLog()[1].count)
    }

    @Test
    fun allReturnsSnapshotIndependentOfFutureChanges() {
        val repo = AuditedSessionRepository()
        val s = Lecture(LocalDate.of(2026, 10, 5), "T", "R", listOf("X"))
        repo.add(s)
        val snapshot = repo.all()
        val s2 = Lecture(LocalDate.of(2026, 10, 6), "T", "R", listOf("X"))
        repo.add(s2)
        assertEquals(1, snapshot.size)
        assertEquals(2, repo.all().size)
    }
}