package attendance;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Standalone regression tests: no JUnit or third-party dependencies are required. */
public final class AttendanceBookTest {
    private static int passed;
    private static void check(boolean condition, String name) {
        if (!condition) { throw new AssertionError(name); }
        System.out.println("PASS " + (++passed) + " - " + name);
    }
    private interface Operation { void run() throws Exception; }
    private static void rejects(Operation operation, String name) throws Exception {
        try { operation.run(); } catch (IllegalArgumentException | IOException expected) {
            check(true, name); return;
        }
        throw new AssertionError("Expected rejection: " + name);
    }
    private static Map<String, Boolean> marks(List<Student> roster, boolean present) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (Student student : roster) { result.put(student.getUsn(), present); }
        return result;
    }
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("attendance-tests-");
        Path file = directory.resolve("attendance.tsv");
        AttendanceBook book = new AttendanceBook(file);
        String course = "CSE-CSBS";
        boolean seeded = true;
        for (String item : AttendanceBook.COURSES) {
            for (int year = 1; year <= 4; year++) {
                seeded &= book.getStudents(item, year).size() == 6;
                seeded &= book.getSessions(item, year).size() == 1;
                seeded &= book.getSessions(item, year).get(0).presentCount() == 4;
            }
        }
        check(seeded, "All 24 cohorts contain six demo students and a sample session");
        check(book.getStudents(course, 2, "  DIYA  ").size() == 1, "Search ignores case and outer spaces");
        check(book.getStudents(course, 2, "does-not-exist").isEmpty(), "Search with no match returns an empty result");
        book.putStudent(null, 8, " test100 ", "  New Student  ", course, 2);
        check(book.getStudents(course, 2, "TEST100").get(0).getName().equals("New Student"), "Add trims names and normalizes USN");
        book.putStudent(null, 7, "TEST101", "Second Student", course, 2);
        check(book.getStudents(course, 2).get(6).getRoll() == 7, "Students sort numerically by roll");
        rejects(() -> book.putStudent(null, 9, "test100", "Duplicate", "Aerospace", 1), "Duplicate USNs rejected across courses");
        rejects(() -> book.putStudent(null, 1, "NEW123", "Duplicate Roll", course, 2), "Duplicate roll rejected in the same cohort");
        book.putStudent(null, 8, "OTHER123", "Other Course", "Aerospace", 2);
        check(book.getStudents("Aerospace", 2).size() == 7, "Same roll allowed in another course");
        rejects(() -> book.putStudent(null, 0, "ZERO123", "Bad", course, 2), "Zero roll rejected");
        rejects(() -> book.putStudent(null, 10000, "BIG123", "Bad", course, 2), "Oversized roll rejected");
        rejects(() -> book.putStudent(null, 9, "BAD123", "  ", course, 2), "Blank name rejected");
        rejects(() -> book.putStudent(null, 9, "BAD123", "Tab\tName", course, 2), "Control character in name rejected");
        rejects(() -> book.putStudent(null, 9, "BAD USN", "Name", course, 2), "Invalid USN rejected");
        rejects(() -> book.getStudents("Unknown", 2), "Unknown course rejected");
        rejects(() -> book.getStudents(course, 5), "Invalid study year rejected");
        List<Student> roster = book.getStudents(course, 2);
        Map<String, Boolean> values = marks(roster, true);
        AttendanceBook.Session first = book.takeAttendance(course, 2, values);
        check(first.getDate().equals(LocalDate.now()) && first.presentCount() == 8, "Today's attendance saves every mark");
        values.put(roster.get(0).getUsn(), false);
        check(first.presentCount() == 8, "Saved marks are protected from caller mutations");
        AttendanceBook.Session second = book.takeAttendance(course, 2, marks(roster, false));
        check(first.getId() != second.getId() && first.getDate().equals(second.getDate()), "Repeated same-day attendance creates distinct sessions");
        check(second.presentCount() == 0, "All-absent attendance is valid");
        book.editAttendance(first.getId(), values);
        check(first.presentCount() == 7 && second.presentCount() == 0, "Editing changes only the selected session");
        rejects(() -> book.editAttendance(99999, values), "Cannot edit a nonexistent session");
        rejects(() -> book.takeAttendance(course, 2, Map.of()), "Incomplete marks rejected");
        Map<String, Boolean> invalid = marks(roster, true); invalid.put("UNKNOWN", true);
        rejects(() -> book.takeAttendance(course, 2, invalid), "Extra student marks rejected");
        invalid.remove("UNKNOWN"); invalid.put(roster.get(0).getUsn(), null);
        rejects(() -> book.takeAttendance(course, 2, invalid), "Null marks rejected");
        book.putStudent("TEST100", 9, "TEST100", "Renamed Student", course, 2);
        check(book.getStudents(course, 2, "TEST100").get(0).getRoll() == 9, "Student name and roll can be edited");
        check(first.getRoster().stream().anyMatch(student -> student.getUsn().equals("TEST100") && student.getName().equals("New Student") && student.getRoll() == 8), "Historical roster survives student edits");
        rejects(() -> book.putStudent("TEST100", 9, "CHANGED", "Name", course, 2), "Existing USN cannot change");
        book.putStudent(null, 10, "LATE100", "Later Student", course, 2);
        book.editAttendance(first.getId(), values);
        check(first.getRoster().size() == 8, "Later students are not inserted into historical sessions");
        AttendanceBook reload = new AttendanceBook(file);
        check(reload.getStudents(course, 2).size() == 9 && reload.getSessions(course, 2).size() == 3, "Students and multiple sessions persist after restart");
        check(reload.getSessions(course, 2).stream().anyMatch(session -> session.getId() == first.getId() && session.presentCount() == 7), "Edited marks persist after restart");
        Path bad = directory.resolve("corrupt.tsv"); Files.writeString(bad, "invalid content");
        rejects(() -> new AttendanceBook(bad), "Corrupt file fails safely");
        check(Files.readString(bad).equals("invalid content"), "Corrupt file is never overwritten with demo data");
        String validData = Files.readString(file);
        Files.writeString(bad, validData.replaceFirst("true", "maybe"));
        rejects(() -> new AttendanceBook(bad), "Invalid saved Boolean is rejected");
        Path empty = directory.resolve("empty.tsv"); Files.writeString(empty, "ATTENDANCE\t1\n");
        AttendanceBook emptyBook = new AttendanceBook(empty);
        check(emptyBook.getStudents(course, 2).isEmpty(), "An intentionally empty database is not reseeded");
        rejects(() -> emptyBook.takeAttendance(course, 2, Map.of()), "Empty attendance cannot be saved");
        Files.delete(file); Files.createDirectory(file); Files.writeString(file.resolve("blocker"), "Force replacement failure");
        int count = book.getStudents(course, 2).size();
        rejects(() -> book.putStudent(null, 11, "FAIL100", "Unsaved", course, 2), "Save failure is surfaced to the caller");
        check(book.getStudents(course, 2).size() == count, "Failed student save rolls back in-memory changes");
        int sessionCount = book.getSessions(course, 2).size();
        rejects(() -> book.takeAttendance(course, 2, marks(book.getStudents(course, 2), true)), "Failed attendance write reports an error");
        check(book.getSessions(course, 2).size() == sessionCount, "Failed attendance creation rolls back");
        rejects(() -> book.editAttendance(first.getId(), marks(first.getRoster(), false)), "Failed attendance edit reports an error");
        check(first.presentCount() == 7, "Failed attendance edit preserves previous marks");
        System.out.println("RESULT: " + passed + " checks passed; 0 failed.");
        // Only test-owned temporary paths are removed, in child-before-parent order.
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); }
        }
    }
}
