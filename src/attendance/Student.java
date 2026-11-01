package attendance; // Groups our three source files into one package.

abstract class Person { // An abstract parent describes the shared idea of a person.
    private final String name; // Private data demonstrates encapsulation; final prevents changes.
    Person(String name) { // The parent constructor receives the person's name.
        this.name = name; // Store the supplied value in this object.
    } // End the constructor.
    public String getName() { return name; } // A getter provides controlled read access.
    public abstract String description(); // Each child must supply its own description.
} // End the abstract parent.

public final class Student extends Person { // Student inherits the name and getter from Person.
    private final int roll; // Roll numbers determine order within a course and year.
    private final String usn; // USN is the permanent unique student identifier.
    private final String course; // Each student belongs to one course.
    private final int year; // Study year is from one to four.
    public Student(int roll, String usn, String name, String course, int year) { // Construct a complete student.
        super(name); // Initialize the name in the parent class.
        this.roll = roll; // Store the roll number.
        this.usn = usn; // Store the USN.
        this.course = course; // Store the course.
        this.year = year; // Store the study year.
    } // End the student constructor.
    public int getRoll() { return roll; } // Read the roll number without allowing direct changes.
    public String getUsn() { return usn; } // Read the permanent USN.
    public String getCourse() { return course; } // Read the course.
    public int getYear() { return year; } // Read the year.
    @Override public String description() { // Override the parent's abstract method.
        return getName() + " (" + usn + ")"; // Describe this particular student.
    } // End the overridden method.
} // End the Student class.
