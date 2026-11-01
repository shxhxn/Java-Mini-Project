# Verification results

Executed on **1 November 2026**, Windows, Oracle JDK **26.0.2**, compiling with `--release 17 -Xlint:all`. Compilation completed with no warnings. Java 17 bytecode compatibility was checked by the compiler; a separate JDK 17 runtime was not used.

## Data tests

`test.bat` ran **41 checks: 41 passed, 0 failed**.

Coverage includes all 24 seeded cohorts; numeric roll ordering; case-insensitive search; valid student creation; duplicate USNs across courses; duplicate rolls inside a cohort; allowed repeated rolls in different cohorts; blank and control-character names; invalid USN, roll, course and year; today's attendance; defensive copying; multiple same-day sessions; all-absent attendance; selective historical editing; missing sessions; incomplete, extra and null marks; student edits; immutable USNs; historical roster preservation after adding or editing students; restart persistence; corrupt-file rejection without overwrite; invalid saved Boolean values; intentionally empty data; rejection of empty attendance; and rollback after failed student, new-session and existing-session writes.

Storage failures were tested by replacing the test-owned destination file with a nonempty directory, making file replacement fail. These tests verify both the error and preservation of in-memory data. They do not simulate sudden power loss.

## Desktop tests

`test-ui.bat` ran **15 checks: 15 passed, 0 failed**, exercising actual Swing components and button actions on the event dispatch thread.

| Checks | Evidence |
| --- | --- |
| 1-6 | All six requested courses appear on the home screen. |
| 7 | Student register contains six students and cannot be edited directly. |
| 8-9 | Present cells are editable Booleans; identity cells are read only. |
| 10 | Choosing No at the discard prompt keeps the attendance form open. |
| 11 | Saving four checked students persists four present marks. |
| 12 | View attendance disables checkbox editing. |
| 13 | Editing the selected historical session saves corrected marks. |
| 14 | A second save today creates a separate session. |
| 15 | Cancelling Add student leaves the register unchanged. |

Eight screenshots in `docs/screenshots/` were captured from the rendered application during the UI run. Test data lives in temporary folders; the user's real data directory is untouched.

## Practical limits

The tests cover the listed behaviours, not every possible machine, operating system, look-and-feel or filesystem. The file lock and midnight date guard are implemented but were not covered by these initial automated checks. Students should run the demo on the computer they will use for evaluation.
