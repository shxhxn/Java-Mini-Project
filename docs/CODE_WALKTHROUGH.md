# Read the code in this order

Start with `Student.java`, then `AttendanceBook.java`, then `AttendanceApp.java`. All nonblank application-code lines are commented. The three files separate the **data objects**, **rules/storage**, and **screen controls**.

## Student.java

`Person` is an abstract class holding a private name. Its constructor sets that name and its getter reads it. Its `description()` method has no implementation: subclasses must provide one.

`Student` extends `Person`. Its constructor calls `super(name)`, then stores roll, USN, course and year. All fields are final. Instead of changing a student's fields, an edit constructs a replacement object. `description()` is overridden to return the name and USN. Keeping the small Person class in the same source file is intentional; only Student is public.

## AttendanceBook.java

`Saveable` is a small interface with a `save()` contract. `AttendanceBook` implements it and owns the current student list, historical sessions and file path. `Session` is a static nested class: it groups session data without requiring a fourth source file. A session stores its ID, course, year, date, historical roster and attendance map.

The constructor loads an existing file. If no file exists, `seedDemo()` creates six students and one yesterday session for every course/year. Existing empty data is left empty. Bad data causes an error instead of being replaced by samples.

`getStudents(course, year)` filters and sorts numerically by roll. The overloaded version takes a search string and uses the overridden `description()` method through a Person reference. `getSessions` filters by cohort and sorts newest date/session first.

`putStudent` normalizes and checks input. It rejects repeated USNs, repeated rolls in one cohort, invalid names and invalid year/course choices. A non-null original USN means edit mode. The method inserts or replaces the student, saves the file, and undoes the change if saving fails.

`takeAttendance` validates the current roster's marks, allocates a distinct session ID, uses today's local date, and saves. `editAttendance` looks up an existing session and validates against that session's original roster. Adding students later therefore cannot accidentally mark them absent in old sessions. Saving only affects the chosen session.

`save` writes a complete UTF-8 snapshot into a temporary file beside the target, then replaces the target. `load` checks the format header, field counts, student identities, duplicate rolls, session IDs and Boolean marks. File errors propagate to the UI. This remains a small-file teaching design; it is not a database or a multi-user server.

## AttendanceApp.java

`main` uses `SwingUtilities.invokeLater` so Swing runs on its event dispatch thread. It selects the system look and feel, obtains a data-folder lock, creates the book and app objects, then displays the window. Closing the window releases the lock. A second instance using the same data folder is rejected.

Navigation is `showCourses` -> `showYears` -> `showMenu`. Each screen uses a shared `page` method for title, subtitle and Back button. `showStudents(false)` is read-only browsing; `showStudents(true)` adds student-management buttons. `studentForm` loops after invalid input so typed values are not lost.

`chooseSession` opens saved history. `attendance(session, editable)` handles three cases: null session means take today's attendance; existing session with editable false means view; existing session with editable true means edit. One shared screen keeps these operations consistent.

`model` returns a small subclass of `DefaultTableModel`. Overriding `getColumnClass` to return `Boolean.class` creates checkboxes in the Present column. `isCellEditable` allows changes only to those checkboxes in create/edit mode. The model listener updates live totals and records unsaved changes.

Saving stops any active cell edit, reads every checkbox into a `Map<String, Boolean>`, asks for confirmation of the totals, then calls the appropriate book method. Successful saving opens history. A storage failure leaves the form open for retry. Back and window close ask before discarding changed marks.

## Try this demonstration

1. Run `run.bat`, choose CSE-CSBS and Year 2, and show the six demo students.
2. Open Edit students and add roll 7, USN `25BTRCB099`, name `Demo New Student`.
3. Try adding that USN again and explain the validation message.
4. Take today's attendance. Use All present, then uncheck two students. Review the totals and save.
5. Take attendance again and save a different set of marks. History now has two separate sessions for today.
6. Edit one saved session. Show that the other session is unchanged.
7. Change a student's name or roll, then view the old session to explain historical snapshots.
8. Close and reopen the application to demonstrate persistence.

## Suggested preparation for five members

These are study suggestions, not claims that anyone has already completed particular work: divide the student model and validation, attendance logic, file handling, Swing screens, and testing/report demonstration among the five members. Each member should still understand the entire three-file flow and run the demonstration independently.

## Viva questions

- Why is Person abstract? It defines shared state and a required behaviour without representing a complete student record.
- Where is runtime polymorphism? In the search method's call to `person.description()`.
- How is overloading different? The compiler selects a method using its parameter list; runtime polymorphism selects an override using the actual object's class.
- Why use a USN as the map key? Roll numbers can repeat across cohorts and can change; a USN is globally unique and fixed after creation.
- Why use a historical roster? Current student edits should not rewrite a past session's names or roll numbers.
- What does final mean? The variable cannot be reassigned; it does not automatically freeze the contents of an object.
- Why no database? A local file is enough for this small, single-user project and demonstrates the circular's file-handling requirement.
- Why can a date have two sessions? The project deliberately permits repeated attendance demonstrations; session IDs distinguish them.
