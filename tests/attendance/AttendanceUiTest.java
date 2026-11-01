package attendance;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.lang.reflect.Field;
import java.util.Comparator;
import javax.imageio.ImageIO;

/** Exercise actual Swing buttons and models on the event dispatch thread. */
public final class AttendanceUiTest {
    private static JFrame frame;
    private static int checks;
    private static <T> T find(Container parent, Class<T> type, String text) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child) && (text == null || child instanceof AbstractButton && ((AbstractButton) child).getText().equals(text))) { return type.cast(child); }
            if (child instanceof Container) {
                T match = find((Container) child, type, text);
                if (match != null) { return match; }
            }
        }
        return null;
    }
    private static void click(String text) {
        JButton button = find(frame, JButton.class, text);
        if (button == null) { throw new AssertionError("Missing button: " + text); }
        button.doClick(); frame.validate();
    }
    private static void check(boolean valid, String name) {
        if (!valid) { throw new AssertionError(name); }
        System.out.println("PASS UI " + (++checks) + " - " + name);
    }
    private static void capture(String name) throws Exception {
        frame.validate();
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); frame.paint(graphics); graphics.dispose();
        ImageIO.write(image, "png", Path.of("docs", "screenshots", name + ".png").toFile());
    }
    private static void respond(String option) {
        Timer timer = new Timer(150, null);
        timer.addActionListener(event -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog && window.isVisible()) {
                    JButton button = find((Container) window, JButton.class, option);
                    if (button != null) { timer.stop(); button.doClick(); return; }
                }
            }
        });
        timer.start();
    }
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("attendance-ui-");
        Files.createDirectories(Path.of("docs", "screenshots"));
        AttendanceBook book = new AttendanceBook(directory.resolve("attendance.tsv"));
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    AttendanceApp app = new AttendanceApp(book);
                    Field field = AttendanceApp.class.getDeclaredField("frame"); field.setAccessible(true); frame = (JFrame) field.get(app);
                    app.show(); capture("01-courses");
                    for (String course : AttendanceBook.COURSES) { check(find(frame, JButton.class, course) != null, "Home includes " + course); }
                    click("CSE-CSBS"); capture("02-years"); click("Year 2"); capture("03-dashboard");
                    click("View students"); JTable students = find(frame, JTable.class, null);
                    check(students.getRowCount() == 6 && !students.isCellEditable(0, 0), "Student register is populated and read only");
                    capture("04-students"); click("Back"); click("Take attendance (today)");
                    JTable table = find(frame, JTable.class, null);
                    check(table.getColumnClass(3) == Boolean.class && table.isCellEditable(0, 3), "Present column is an editable Boolean checkbox");
                    check(!table.isCellEditable(0, 1), "USN cannot be edited in attendance");
                    table.setValueAt(true, 0, 3); table.setValueAt(true, 1, 3); table.setValueAt(true, 3, 3); table.setValueAt(true, 4, 3);
                    capture("05-take-attendance");
                    respond("No"); click("Back");
                    check(find(frame, JButton.class, "Save new session") != null, "Discard No preserves the attendance form");
                    respond("OK"); click("Save new session");
                    check(book.getSessions("CSE-CSBS", 2).get(0).presentCount() == 4, "Saving from checkbox table persists four present students");
                    capture("06-sessions"); click("View selected session");
                    check(!find(frame, JTable.class, null).isCellEditable(0, 3), "View attendance is read only");
                    capture("07-view-attendance"); click("Back"); click("Back"); click("Edit attendance"); click("Edit selected session");
                    click("All present"); capture("08-edit-attendance"); respond("OK"); click("Save changes");
                    check(book.getSessions("CSE-CSBS", 2).get(0).presentCount() == 6, "Edit existing attendance saves changed checkbox marks");
                    click("Back"); click("Take attendance (today)"); click("All present"); click("All absent"); respond("OK"); click("Save new session");
                    check(book.getSessions("CSE-CSBS", 2).size() == 3, "Second UI save creates a distinct same-day session");
                    click("Back"); click("Edit students"); respond("Cancel"); click("Add student");
                    check(book.getStudents("CSE-CSBS", 2).size() == 6, "Cancelling Add student preserves the register");
                    System.out.println("RESULT: " + checks + " UI checks passed; 0 failed.");
                } catch (Exception error) { throw new RuntimeException(error); }
                finally { if (frame != null) { frame.dispose(); } }
            });
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); }
            }
        }
    }
}
