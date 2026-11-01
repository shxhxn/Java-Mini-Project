package attendance; // Place the desktop interface in our application package.

import javax.swing.*; // Swing supplies windows, buttons, dialogs and tables.
import javax.swing.table.DefaultTableModel; // A table model stores the values displayed by JTable.
import java.awt.*; // Layouts, colours, fonts and dimensions come from AWT.
import java.awt.event.*; // Listen for window closing events.
import java.io.IOException; // Handle storage failures without crashing the UI.
import java.nio.channels.*; // Lock the data folder against two simultaneous app instances.
import java.nio.file.*; // Locate the local data folder and files.
import java.time.LocalDate; // Show today's date in the attendance form.
import java.util.LinkedHashMap; // Keep attendance marks in student order.
import java.util.List; // Refer unambiguously to the collection List, not java.awt.List.
import java.util.Map; // Associate each USN with a Boolean attendance mark.

public final class AttendanceApp { // Main application and screen navigation live in one file.
    private final JFrame frame = new JFrame("Student Attendance Management System"); // Create the main desktop window.
    private final AttendanceBook book; // Use the data layer instead of storing records inside widgets.
    private String course; // Remember the currently selected course.
    private int year; // Remember the currently selected study year.
    private boolean dirty; // True while an attendance checkbox change has not been saved.
    private JPanel content; // This panel is replaced when a different screen opens.

    public AttendanceApp(AttendanceBook book) { // Receive the data layer through the constructor.
        this.book = book; // Store the supplied book for button actions.
        frame.setSize(1000, 700); frame.setMinimumSize(new Dimension(840, 600)); // Keep tables and buttons readable.
        frame.setLocationRelativeTo(null); // Centre the window on the display.
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // Confirm before discarding unsaved attendance.
        frame.addWindowListener(new WindowAdapter() { // Override only the closing callback we need.
            @Override public void windowClosing(WindowEvent event) { if (mayLeave()) { frame.dispose(); } } // Let the user keep unsaved work.
        }); // Finish registering the window listener.
        showCourses(); // Begin with the six course choices.
    } // End the constructor.
    public void show() { frame.setVisible(true); } // Display the prepared main window.
    private boolean mayLeave() { // Centralize unsaved-attendance confirmation.
        return !dirty || JOptionPane.showConfirmDialog(frame, "Discard unsaved attendance changes?", "Unsaved attendance", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; // Continue only on explicit discard.
    } // End the navigation guard.
    private void page(String title, String subtitle, Runnable back) { // Create the shared page frame.
        dirty = false; // A newly opened screen begins without pending changes.
        content = new JPanel(new BorderLayout(16, 16)); // Separate heading, main content and actions.
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28)); // Leave comfortable space at the edges.
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 8)); // Stack navigation, title and helper text.
        if (back != null) { heading.add(button("Back", () -> { if (mayLeave()) { back.run(); } })); } // Return one navigation level.
        JLabel label = new JLabel(title); label.setFont(new Font("SansSerif", Font.BOLD, 24)); // Show the page title prominently.
        heading.add(label); heading.add(new JLabel(subtitle)); // Explain the current context in plain language.
        content.add(heading, BorderLayout.NORTH); frame.setContentPane(content); // Install the screen's root panel.
    } // End page preparation.
    private void refresh() { frame.revalidate(); frame.repaint(); } // Redraw after adding the screen's controls.
    private JButton button(String text, Runnable action) { // Reuse consistent button construction.
        JButton button = new JButton(text); // Create a labelled action button.
        button.setFont(new Font("SansSerif", Font.PLAIN, 16)); // Keep button text readable.
        button.addActionListener(event -> { // Call the action when the user clicks.
            try { action.run(); } catch (IllegalArgumentException error) { error(error.getMessage()); } // Show input errors as dialogs.
        }); // Finish the action listener.
        return button; // Give the configured button to the caller.
    } // End button creation.
    private void error(String message) { JOptionPane.showMessageDialog(frame, message, "Please check", JOptionPane.ERROR_MESSAGE); } // Present a recoverable error.
    private String cohort() { return course + " / Year " + year; } // Format the current course and year.
    private JPanel actions() { return new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6)); } // Make a row for action buttons.
    private void showCourses() { // Display the application's home screen.
        page("Student Attendance", "Choose a course to view students and manage date-wise attendance.", null); // Set the page heading.
        JPanel grid = new JPanel(new GridLayout(3, 2, 18, 18)); // Display six equally sized course buttons.
        for (String choice : AttendanceBook.COURSES) { grid.add(button(choice, () -> { course = choice; showYears(); })); } // Navigate into the chosen course.
        content.add(grid, BorderLayout.CENTER); // Put the course choices in the main area.
        content.add(new JLabel("First run includes fictional DEMO students and yesterday's sample attendance in every year."), BorderLayout.SOUTH); // Identify seeded records clearly.
        refresh(); // Show the completed screen.
    } // End course selection.
    private void showYears() { // Display years within the selected course.
        page(course, "Choose a study year.", this::showCourses); // Keep a route back to all courses.
        JPanel grid = new JPanel(new GridLayout(2, 2, 18, 18)); // Arrange the four year buttons.
        for (int number = 1; number <= 4; number++) { // Create one button per study year.
            final int selectedYear = number; // Lambdas capture a stable value for each button.
            grid.add(button("Year " + number, () -> { year = selectedYear; showMenu(); })); // Open the chosen cohort.
        } // End year button creation.
        content.add(grid, BorderLayout.CENTER); refresh(); // Finish the year selection screen.
    } // End year selection.
    private void showMenu() { // Display the requested operations for a cohort.
        page(cohort(), book.getStudents(course, year).size() + " students | " + book.getSessions(course, year).size() + " saved sessions", this::showYears); // Show current totals.
        JPanel grid = new JPanel(new GridLayout(3, 2, 16, 16)); // Use a simple menu of large buttons.
        grid.add(button("View students", () -> showStudents(false))); // Open the read-only student register.
        grid.add(button("Edit students", () -> showStudents(true))); // Open add and edit controls.
        grid.add(button("Take attendance (today)", () -> attendance(null, true))); // Start a fresh attendance session.
        grid.add(button("View attendance", () -> chooseSession(false))); // Select a historical session to inspect.
        grid.add(button("Edit attendance", () -> chooseSession(true))); // Select an existing session to correct.
        content.add(grid, BorderLayout.CENTER); // Install the operations menu.
        content.add(new JLabel("Each new attendance creates a separate session, including repeated sessions on the same day."), BorderLayout.SOUTH); // Explain repeated attendance.
        refresh(); // Display the dashboard.
    } // End the cohort menu.
    private JTable table(DefaultTableModel model) { // Apply a shared table configuration.
        JTable table = new JTable(model); table.setRowHeight(34); // Give each student a readable row.
        table.setFont(new Font("SansSerif", Font.PLAIN, 15)); // Use a comfortable data font.
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 15)); // Distinguish column headings.
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Editing applies to exactly one student.
        table.getTableHeader().setReorderingAllowed(false); // Keep column positions stable for the form.
        table.setFillsViewportHeight(true); // Fill the table area even with few students.
        return table; // Return the configured widget.
    } // End table setup.
    private DefaultTableModel model(boolean attendance, boolean editable) { // Create either student rows or checkbox attendance rows.
        String[] columns = attendance ? new String[]{"Roll", "USN", "Name", "Present"} : new String[]{"Roll", "USN", "Name"}; // Select the columns.
        return new DefaultTableModel(columns, 0) { // Override behaviour in a small anonymous subclass.
            @Override public boolean isCellEditable(int row, int column) { return attendance && editable && column == 3; } // Only attendance checkboxes may be edited directly.
            @Override public Class<?> getColumnClass(int column) { return column == 0 ? Integer.class : column == 3 ? Boolean.class : String.class; } // Boolean columns use Swing checkboxes.
        }; // Return the specialized model.
    } // End model construction.
    private void showStudents(boolean editable) { // Show and optionally manage the current register.
        page(cohort() + " / Students", editable ? "Add a student or select a row and edit its name or roll number. USNs stay fixed." : "Students are listed in roll-number order. Search by name or USN.", this::showMenu); // Explain the mode.
        DefaultTableModel model = model(false, false); JTable table = table(model); // Create the read-only data grid.
        JPanel main = new JPanel(new BorderLayout(8, 8)); JPanel searchRow = actions(); JTextField query = new JTextField(24); // Prepare search and table panels.
        Runnable search = () -> { // A shared action supports the Search button and Enter key.
            model.setRowCount(0); // Remove old search results.
            for (Student student : book.getStudents(course, year, query.getText())) { model.addRow(new Object[]{student.getRoll(), student.getUsn(), student.getName()}); } // Fill matching results in roll order.
        }; // End the search callback.
        searchRow.add(new JLabel("Name or USN")); searchRow.add(query); searchRow.add(button("Search", search)); // Add search controls.
        query.addActionListener(event -> search.run()); // Pressing Enter also searches.
        main.add(searchRow, BorderLayout.NORTH); main.add(new JScrollPane(table), BorderLayout.CENTER); // Keep the search above a scrollable table.
        content.add(main, BorderLayout.CENTER); // Install the register panel.
        if (editable) { // Only show mutation actions in edit mode.
            JPanel controls = actions(); controls.add(button("Add student", () -> studentForm(null))); // Open an empty student form.
            controls.add(button("Edit selected student", () -> { // Open the selected student's form.
                int row = table.getSelectedRow(); // Ask the table which student is selected.
                if (row < 0) { error("Select a student row first."); return; } // Explain a missing selection.
                String usn = model.getValueAt(row, 1).toString(); // Read the permanent identifier.
                Student selected = book.getStudents(course, year).stream().filter(student -> student.getUsn().equals(usn)).findFirst().orElseThrow(); // Find the matching domain object.
                studentForm(selected); // Open the prefilled form.
            })); // End the edit callback.
            content.add(controls, BorderLayout.SOUTH); // Place editing buttons below the register.
        } // End edit-mode controls.
        search.run(); refresh(); // Populate and display the register.
    } // End student browsing.
    private void studentForm(Student student) { // A modal form handles both add and edit operations.
        JTextField roll = new JTextField(student == null ? "" : String.valueOf(student.getRoll())); // Prefill the existing roll.
        JTextField usn = new JTextField(student == null ? "" : student.getUsn()); // Prefill the existing identifier.
        JTextField name = new JTextField(student == null ? "" : student.getName()); // Prefill the existing name.
        usn.setEditable(student == null); // Prevent changing permanent identity after creation.
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12)); // Arrange labels beside fields.
        form.add(new JLabel("Roll number")); form.add(roll); form.add(new JLabel("USN")); form.add(usn); form.add(new JLabel("Full name")); form.add(name); // Populate the form.
        while (JOptionPane.showConfirmDialog(frame, form, student == null ? "Add student" : "Edit student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) { // Keep inputs after an error.
            try { // Convert input, validate it and save it through the data layer.
                book.putStudent(student == null ? null : student.getUsn(), Integer.parseInt(roll.getText().trim()), usn.getText(), name.getText(), course, year); // Add or replace the student.
                showStudents(true); return; // Refresh only after a successful save.
            } catch (NumberFormatException error) { error("Roll number must be a whole number from 1 to 9999."); } // Explain nonnumeric rolls.
            catch (IllegalArgumentException | IOException error) { error(error.getMessage()); } // Preserve the form on validation or storage failure.
        } // End the form confirmation loop.
    } // End student entry.
    private void chooseSession(boolean editable) { // Let the user choose an already saved attendance session.
        page(cohort() + (editable ? " / Edit attendance" : " / View attendance"), "Choose a date and session. Historical rosters preserve names and rolls as originally recorded.", this::showMenu); // Explain historical data.
        List<AttendanceBook.Session> sessions = book.getSessions(course, year); JPanel panel = new JPanel(new BorderLayout(12, 12)); // Retrieve only this cohort's sessions.
        if (sessions.isEmpty()) { panel.add(new JLabel("No attendance yet. Use Take attendance (today) to create a session.")); } // Explain an empty history.
        else { // Offer session selection only when records exist.
            JList<AttendanceBook.Session> list = new JList<>(sessions.toArray(new AttendanceBook.Session[0])); // Use each session's toString text.
            list.setFont(new Font("SansSerif", Font.PLAIN, 18)); list.setFixedCellHeight(46); list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); list.setSelectedIndex(0); // Make sessions easy to choose.
            panel.add(new JScrollPane(list), BorderLayout.CENTER); // Scroll when many sessions exist.
            panel.add(button(editable ? "Edit selected session" : "View selected session", () -> { // Open the chosen historical session.
                if (list.getSelectedValue() != null) { attendance(list.getSelectedValue(), editable); } // Guard against an empty selection.
            }), BorderLayout.SOUTH); // Put the open action below the list.
        } // End history population.
        content.add(panel, BorderLayout.CENTER); refresh(); // Display the session picker.
    } // End historical selection.
    private void attendance(AttendanceBook.Session session, boolean editable) { // One screen supports create, view and edit.
        boolean fresh = session == null; List<Student> roster = fresh ? book.getStudents(course, year) : session.getRoster(); // Choose current or historical students.
        page(cohort() + " / " + (fresh ? LocalDate.now() : session.getDate()), fresh ? "New session - checked means Present, unchecked means Absent. All students start Absent." : "Session #" + session.getId() + " - " + (editable ? "edit the saved marks, then save changes." : "saved attendance, view only."), () -> { if (fresh) { showMenu(); } else { chooseSession(editable); } }); // Set context and return path.
        DefaultTableModel model = model(true, editable); JTable table = table(model); // Create the checkbox table.
        for (Student student : roster) { model.addRow(new Object[]{student.getRoll(), student.getUsn(), student.getName(), !fresh && session.isPresent(student.getUsn())}); } // Start fresh marks absent or restore saved values.
        JPanel main = new JPanel(new BorderLayout(8, 8)); JLabel count = new JLabel(); // Prepare live present and absent totals.
        Runnable totals = () -> { // Recalculate counts after every checkbox edit.
            int present = 0; for (int row = 0; row < model.getRowCount(); row++) { if (Boolean.TRUE.equals(model.getValueAt(row, 3))) { present++; } } // Count selected rows.
            count.setText("Present: " + present + "    Absent: " + (roster.size() - present) + "    Total: " + roster.size()); // Display current totals.
        }; // End totals calculation.
        model.addTableModelListener(event -> { dirty = true; totals.run(); }); // Track unsaved changes and update totals.
        main.add(new JScrollPane(table), BorderLayout.CENTER); main.add(count, BorderLayout.SOUTH); // Keep totals directly under the roster.
        content.add(main, BorderLayout.CENTER); // Install the attendance table.
        if (editable) { // Only create/edit screens may modify marks.
            JPanel controls = actions(); // Prepare bulk actions and save.
            controls.add(button("All present", () -> { for (int row = 0; row < model.getRowCount(); row++) { model.setValueAt(true, row, 3); } })); // Check every student.
            controls.add(button("All absent", () -> { for (int row = 0; row < model.getRowCount(); row++) { model.setValueAt(false, row, 3); } })); // Uncheck every student.
            JButton save = button(fresh ? "Save new session" : "Save changes", () -> { // Commit the user's final selections.
                if (table.isEditing() && !table.getCellEditor().stopCellEditing()) { return; } // Commit an active checkbox editor first.
                if (fresh && !LocalDate.now().toString().equals(frame.getRootPane().getClientProperty("attendanceDate"))) { error("The date changed. Reopen Take attendance to record the new day."); return; } // Avoid silently saving under a different day at midnight.
                Map<String, Boolean> marks = new LinkedHashMap<>(); // Build the domain input from table values.
                for (int row = 0; row < roster.size(); row++) { marks.put(roster.get(row).getUsn(), (Boolean) model.getValueAt(row, 3)); } // Map each USN to its checkbox.
                if (JOptionPane.showConfirmDialog(frame, count.getText() + "\nSave this attendance?", "Confirm attendance", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) { return; } // Make all-absent saves deliberate too.
                try { // Save through the validated data layer.
                    if (fresh) { book.takeAttendance(course, year, marks); } else { book.editAttendance(session.getId(), marks); } // Create or update the intended session.
                    dirty = false; chooseSession(false); // Show saved history after success.
                } catch (IOException error) { error("Attendance was not saved. " + error.getMessage()); } // Leave unsaved checkboxes available for retry.
            }); // End the save callback.
            save.setEnabled(!roster.isEmpty()); controls.add(save); content.add(controls, BorderLayout.SOUTH); // Disable saving an empty cohort.
        } // End mutation controls.
        frame.getRootPane().putClientProperty("attendanceDate", LocalDate.now().toString()); // Remember the date on which the fresh form opened.
        totals.run(); refresh(); // Display the completed attendance form.
    } // End attendance screen.

    public static void main(String[] args) { // Program entry point when launched from the terminal.
        SwingUtilities.invokeLater(() -> { // Construct and operate Swing components on the event dispatch thread.
            FileChannel channel = null; // Keep the lock channel alive for the lifetime of the window.
            try { // Explain setup errors through a desktop dialog.
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // Use familiar operating-system controls.
                Path directory = Path.of(System.getProperty("attendance.dataDir", "data")).toAbsolutePath(); // Allow tests or demos to use another folder.
                Files.createDirectories(directory); // Create the local data directory if necessary.
                channel = FileChannel.open(directory.resolve("attendance.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE); // Open the shared lock file.
                FileLock lock = channel.tryLock(); // Prevent conflicting writes from two running instances.
                if (lock == null) { throw new IOException("This data folder is already open in another app window."); } // Stop an unsafe second instance.
                AttendanceApp app = new AttendanceApp(new AttendanceBook(directory.resolve("attendance.tsv"))); // Load records before showing the UI.
                FileChannel heldChannel = channel; // Capture the resource for the close callback.
                app.frame.addWindowListener(new WindowAdapter() { // Release the lock after the window is actually disposed.
                    @Override public void windowClosed(WindowEvent event) { // Run after close confirmation succeeds.
                        try { lock.release(); heldChannel.close(); } catch (IOException error) { System.err.println(error.getMessage()); } // Release operating-system resources.
                    } // End the close callback.
                }); // Finish resource cleanup registration.
                app.show(); // Display the ready application.
            } catch (Exception error) { // Catch startup failures such as invalid data or an unavailable lock.
                if (channel != null) { try { channel.close(); } catch (IOException ignored) { /* The main startup error is reported below. */ } } // Close any partially initialized resource.
                JOptionPane.showMessageDialog(null, "Could not start attendance app.\n" + error.getMessage(), "Startup error", JOptionPane.ERROR_MESSAGE); // Give the user an actionable error.
            } // End startup error handling.
        }); // End the event-dispatch-thread task.
    } // End main.
} // End the desktop application.
