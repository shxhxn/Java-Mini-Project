package attendance; // Keep the data layer in the application's package.

import java.io.IOException; // Represent file read and write failures.
import java.nio.charset.StandardCharsets; // Save text using portable UTF-8 encoding.
import java.nio.file.*; // Use Path, Files and safe file replacement options.
import java.time.LocalDate; // Store dates without subject or time information.
import java.util.*; // Use lists, maps, sets, sorting and string helpers.

interface Saveable { // An interface specifies a capability independently of implementation.
    void save() throws IOException; // Any implementing class must know how to save itself.
} // End the interface.

@SuppressWarnings("auxiliaryclass") // Person intentionally shares Student.java to keep only three source files.
public final class AttendanceBook implements Saveable { // Own students, sessions and persistence.
    public static final List<String> COURSES = List.of( // One shared, unmodifiable course list.
        "CSE-CSBS", "CSE-General", "CSE - PWC", // First three choices requested by the team.
        "CSE - Cybersecurity", "Aerospace", "Mechanical"); // Remaining three choices.
    private final List<Student> students = new ArrayList<>(); // Mutable list owned by this class.
    private final List<Session> sessions = new ArrayList<>(); // Keep each attendance attempt separately.
    private final Path file; // The snapshot file used by this book.

    public static final class Session { // A small nested class avoids another source file.
        private final int id; // A unique number distinguishes sessions on the same date.
        private final String course; // Course selected when attendance was taken.
        private final int year; // Year selected when attendance was taken.
        private final LocalDate date; // The session's original date never changes during editing.
        private final List<Student> roster; // Snapshot protects old names and rolls from later edits.
        private Map<String, Boolean> marks; // Map each roster USN to present or absent.
        private Session(int id, String course, int year, LocalDate date, List<Student> roster, Map<String, Boolean> marks) { // Build a session.
            this.id = id; this.course = course; this.year = year; this.date = date; // Store session identity.
            this.roster = List.copyOf(roster); this.marks = new LinkedHashMap<>(marks); // Copy mutable inputs.
        } // End the constructor.
        public int getId() { return id; } // Read the unique session number.
        public LocalDate getDate() { return date; } // Read the original attendance date.
        public List<Student> getRoster() { return roster; } // Return the immutable historical roster.
        public boolean isPresent(String usn) { return Boolean.TRUE.equals(marks.get(usn)); } // Read a mark safely.
        public long presentCount() { return marks.values().stream().filter(Boolean::booleanValue).count(); } // Count checked students.
        @Override public String toString() { // Supply readable text for Swing's session selector.
            return date + " | Session #" + id + " | " + presentCount() + "/" + roster.size() + " present"; // Summarize a session.
        } // End the display method.
    } // End the nested Session class.

    public AttendanceBook(Path file) throws IOException { // Load saved data or create the first demonstration dataset.
        this.file = file.toAbsolutePath(); // Use one stable absolute path for all file operations.
        if (Files.exists(this.file)) { load(); } // Existing data must load successfully; never silently replace it.
        else { seedDemo(); save(); } // Only a missing file triggers demonstration data.
    } // End the book constructor.

    public List<Student> getStudents(String course, int year) { // First overloaded version lists one cohort.
        checkCohort(course, year); // Reject unsupported course/year combinations.
        List<Student> result = new ArrayList<>(); // Build a separate result list.
        for (Student student : students) { // Visit every stored student.
            if (student.getCourse().equals(course) && student.getYear() == year) { result.add(student); } // Select the cohort.
        } // End the filtering loop.
        result.sort(Comparator.comparingInt(Student::getRoll)); // Sort by numeric roll number.
        return List.copyOf(result); // Prevent callers from changing the returned list.
    } // End the cohort query.
    public List<Student> getStudents(String course, int year, String query) { // Overloading adds an optional search argument.
        String key = query.trim().toLowerCase(Locale.ROOT); // Normalize case and surrounding spaces.
        List<Student> result = new ArrayList<>(); // Collect matching students.
        for (Student student : getStudents(course, year)) { // Reuse the two-argument method.
            Person person = student; // A parent reference points to a Student object.
            if (person.description().toLowerCase(Locale.ROOT).contains(key)) { result.add(student); } // Runtime dispatch calls Student.description.
        } // End the search loop.
        return List.copyOf(result); // Expose only a read-only result.
    } // End the overloaded search method.
    public List<Session> getSessions(String course, int year) { // List saved sessions for a cohort.
        checkCohort(course, year); // Validate the selected cohort.
        List<Session> result = new ArrayList<>(); // Prepare a separate list for the screen.
        for (Session session : sessions) { // Examine all saved sessions.
            if (session.course.equals(course) && session.year == year) { result.add(session); } // Keep matching sessions.
        } // End the session loop.
        result.sort(Comparator.comparing(Session::getDate).thenComparingInt(Session::getId).reversed()); // Newest sessions first.
        return List.copyOf(result); // Protect the internal list.
    } // End the session query.

    public void putStudent(String originalUsn, int roll, String usn, String name, String course, int year) throws IOException { // Add or edit a student.
        checkCohort(course, year); // Confirm a valid course and year.
        usn = usn.trim().toUpperCase(Locale.ROOT); name = name.trim(); // Normalize user input.
        require(roll > 0 && roll <= 9999, "Roll number must be between 1 and 9999."); // Reject unusable roll numbers.
        require(usn.matches("[A-Z0-9-]{3,24}"), "USN must contain 3-24 letters, digits or hyphens."); // Keep identifiers simple.
        require(!name.isEmpty() && name.length() <= 80 && name.chars().noneMatch(Character::isISOControl), "Name must contain 1-80 characters without control characters."); // Validate names and protect file separators.
        int index = -1; // A negative index means this is a new student.
        for (int i = 0; i < students.size(); i++) { // Check identifiers and roll numbers against existing students.
            Student existing = students.get(i); // Read the current student.
            if (existing.getUsn().equals(originalUsn)) { // Locate the record being edited.
                index = i; // Remember where it belongs in the list.
                require(existing.getCourse().equals(course) && existing.getYear() == year, "Student belongs to another cohort."); // Disallow accidental transfers.
                continue; // Do not treat the same student as a duplicate.
            } // End the edit-record check.
            require(!existing.getUsn().equals(usn), "That USN already exists."); // USNs are unique across the entire app.
            require(!(existing.getCourse().equals(course) && existing.getYear() == year && existing.getRoll() == roll), "That roll number is already used in this cohort."); // Rolls are unique within a cohort.
        } // End duplicate checking.
        require(originalUsn == null || (index >= 0 && originalUsn.equals(usn)), "An existing student's USN cannot be changed."); // Preserve permanent identity.
        Student replacement = new Student(roll, usn, name, course, year); // Construct an immutable replacement.
        Student previous = index < 0 ? null : students.get(index); // Remember old data for a possible rollback.
        if (index < 0) { students.add(replacement); } else { students.set(index, replacement); } // Apply the change in memory.
        try { save(); } catch (IOException error) { // Persist before reporting success.
            if (index < 0) { students.remove(replacement); } else { students.set(index, previous); } // Undo an unsuccessful save.
            throw error; // Let the UI explain the storage failure.
        } // End persistence handling.
    } // End student editing.

    public Session takeAttendance(String course, int year, Map<String, Boolean> marks) throws IOException { // Public action always records today's date.
        List<Student> roster = getStudents(course, year); // Capture the current roll-sorted roster.
        validateMarks(roster, marks); // Require one explicit Boolean mark per student.
        int id = sessions.stream().mapToInt(Session::getId).max().orElse(0) + 1; // Allocate a new session number.
        Session session = new Session(id, course, year, LocalDate.now(), roster, marks); // Create a separate same-day session.
        sessions.add(session); // Tentatively add it to memory.
        try { save(); } catch (IOException error) { sessions.remove(session); throw error; } // Roll back if saving fails.
        return session; // Return the saved session for confirmation.
    } // End attendance creation.
    public void editAttendance(int id, Map<String, Boolean> marks) throws IOException { // Change only a session that already exists.
        Session session = sessions.stream().filter(item -> item.id == id).findFirst().orElseThrow(() -> new IllegalArgumentException("Select an existing session.")); // Reject missing sessions.
        validateMarks(session.roster, marks); // Validate against the historical roster, not today's students.
        Map<String, Boolean> previous = session.marks; // Keep the original values for rollback.
        session.marks = new LinkedHashMap<>(marks); // Apply a defensive copy of the new marks.
        try { save(); } catch (IOException error) { session.marks = previous; throw error; } // Restore old marks on failure.
    } // End attendance editing.
    private static void validateMarks(List<Student> roster, Map<String, Boolean> marks) { // Shared attendance validation.
        require(!roster.isEmpty(), "Add students before taking attendance."); // Empty attendance cannot be saved.
        require(marks != null && marks.size() == roster.size(), "Mark every student in this session."); // Reject missing or extra marks.
        for (Student student : roster) { require(marks.get(student.getUsn()) != null, "Every student needs a present or absent value."); } // Reject null or unknown keys.
    } // End mark validation.
    private static void checkCohort(String course, int year) { // Keep all cohort checks in one place.
        require(COURSES.contains(course) && year >= 1 && year <= 4, "Choose a listed course and year 1-4."); // Validate fixed choices.
    } // End cohort validation.
    private static void require(boolean valid, String message) { // A small helper avoids repeating exception logic.
        if (!valid) { throw new IllegalArgumentException(message); } // Report an understandable validation error.
    } // End the validation helper.

    @Override public void save() throws IOException { // Implement the Saveable interface using a UTF-8 snapshot.
        Files.createDirectories(file.getParent()); // Create the data folder on first use.
        List<String> lines = new ArrayList<>(); // Build the complete snapshot before touching disk.
        lines.add("ATTENDANCE\t1"); // Version header identifies our simple file format.
        for (Student student : students) { lines.add("S\t" + studentLine(student)); } // Store the current student register.
        for (Session session : sessions) { // Store every historical attendance session.
            lines.add("A\t" + session.id + "\t" + session.course + "\t" + session.year + "\t" + session.date); // Session header.
            for (Student student : session.roster) { // Save each historical student and mark together.
                lines.add("M\t" + session.id + "\t" + studentLine(student) + "\t" + session.isPresent(student.getUsn())); // Historical snapshot row.
            } // End the historical roster loop.
        } // End the session loop.
        Path temporary = Files.createTempFile(file.getParent(), "attendance-", ".tmp"); // Write next to the destination for atomic replacement.
        try { // Always clean up temporary files, even after a failure.
            Files.write(temporary, lines, StandardCharsets.UTF_8); // Write the complete new file first.
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); } // Replace atomically when supported.
            catch (AtomicMoveNotSupportedException error) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); } // Fall back on other filesystems.
        } finally { Files.deleteIfExists(temporary); } // Remove an unused temporary file.
    } // End file saving.
    private static String studentLine(Student student) { // Convert one student into tab-separated fields.
        return student.getRoll() + "\t" + student.getUsn() + "\t" + student.getName() + "\t" + student.getCourse() + "\t" + student.getYear(); // Fields cannot contain tabs.
    } // End the row formatter.
    private static Student readStudent(String[] fields, int start) { // Rebuild a student from five consecutive fields.
        int roll = Integer.parseInt(fields[start]); int year = Integer.parseInt(fields[start + 4]); // Parse numeric fields.
        String usn = fields[start + 1]; String name = fields[start + 2]; String course = fields[start + 3]; // Read text fields.
        checkCohort(course, year); // Reject invalid saved cohorts.
        require(roll > 0 && roll <= 9999 && usn.matches("[A-Z0-9-]{3,24}") && !name.isBlank() && name.length() <= 80 && name.chars().noneMatch(Character::isISOControl), "Invalid saved student."); // Validate saved fields.
        return new Student(roll, usn, name, course, year); // Return a valid immutable object.
    } // End student parsing.
    private void load() throws IOException { // Strict loading prevents damaged data being silently overwritten.
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8); // Read the whole small local file.
        try { // Convert parsing failures into an actionable storage error.
            require(!lines.isEmpty() && lines.get(0).equals("ATTENDANCE\t1"), "Unsupported or missing file header."); // Check the format version.
            Set<String> usns = new HashSet<>(); Set<String> rolls = new HashSet<>(); Set<Integer> ids = new HashSet<>(); // Track duplicates.
            int index = 1; // Begin after the format header.
            while (index < lines.size() && lines.get(index).startsWith("S\t")) { // Student records appear first.
                String[] fields = lines.get(index++).split("\t", -1); require(fields.length == 6, "Malformed student row."); // Check row width.
                Student student = readStudent(fields, 1); // Parse the current register row.
                require(usns.add(student.getUsn()) && rolls.add(student.getCourse() + "/" + student.getYear() + "/" + student.getRoll()), "Duplicate student or roll."); // Reject ambiguous records.
                students.add(student); // Keep the validated student.
            } // End the current register loop.
            while (index < lines.size()) { // Read a session header followed by its marks.
                String[] fields = lines.get(index++).split("\t", -1); // Split the next header.
                require(fields.length == 5 && fields[0].equals("A"), "Malformed session header."); // Reject unknown records.
                int id = Integer.parseInt(fields[1]); int year = Integer.parseInt(fields[3]); // Parse the identifiers.
                String course = fields[2]; LocalDate date = LocalDate.parse(fields[4]); // Parse course and ISO date.
                checkCohort(course, year); require(id > 0 && id < Integer.MAX_VALUE && ids.add(id), "Invalid or duplicate session number."); // Check header validity.
                List<Student> roster = new ArrayList<>(); Map<String, Boolean> marks = new LinkedHashMap<>(); Set<Integer> sessionRolls = new HashSet<>(); // Accumulate this session.
                while (index < lines.size() && lines.get(index).startsWith("M\t")) { // Read all marks belonging to it.
                    String[] row = lines.get(index++).split("\t", -1); // Split one attendance record.
                    require(row.length == 8 && Integer.parseInt(row[1]) == id, "Malformed attendance row."); // Ensure the row belongs here.
                    Student student = readStudent(row, 2); // Restore the historical student snapshot.
                    require(student.getCourse().equals(course) && student.getYear() == year && usns.contains(student.getUsn()) && !marks.containsKey(student.getUsn()) && sessionRolls.add(student.getRoll()), "Invalid session roster."); // Check roster identity.
                    require(row[7].equals("true") || row[7].equals("false"), "Invalid attendance value."); // Do not silently interpret damaged text as absence.
                    roster.add(student); marks.put(student.getUsn(), Boolean.parseBoolean(row[7])); // Store this historical mark.
                } // End the marks loop.
                validateMarks(roster, marks); roster.sort(Comparator.comparingInt(Student::getRoll)); // Validate and restore roll order.
                sessions.add(new Session(id, course, year, date, roster, marks)); // Store the complete session.
            } // End session parsing.
        } catch (RuntimeException error) { // Catch bad integers, dates and validation errors.
            throw new IOException("Cannot read " + file + ". Your file was not changed. " + error.getMessage(), error); // Preserve evidence and stop startup.
        } // End parsing error handling.
    } // End file loading.
    private void seedDemo() { // Give every course/year a useful first-run demonstration.
        String[] names = {"Aarav Rao", "Diya Shah", "Ishaan Das", "Meera Nair", "Rohan Kumar", "Sara Ali"}; // Clearly fictional sample names.
        for (int course = 0; course < COURSES.size(); course++) { // Visit all six courses.
            for (int year = 1; year <= 4; year++) { // Visit all four years.
                for (int roll = 1; roll <= names.length; roll++) { // Create six students in this cohort.
                    students.add(new Student(roll, "DEMO" + (course + 1) + year + String.format(Locale.ROOT, "%03d", roll), names[roll - 1], COURSES.get(course), year)); // Unique demonstration USN.
                } // End sample student creation.
                List<Student> roster = getStudents(COURSES.get(course), year); Map<String, Boolean> marks = new LinkedHashMap<>(); // Prepare historical sample attendance.
                for (Student student : roster) { marks.put(student.getUsn(), student.getRoll() % 3 != 0); } // Four present and two absent per cohort.
                sessions.add(new Session(sessions.size() + 1, COURSES.get(course), year, LocalDate.now().minusDays(1), roster, marks)); // Add yesterday's demonstration session.
            } // End the year loop.
        } // End the course loop.
    } // End demonstration seeding.
} // End the AttendanceBook class.
