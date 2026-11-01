# Java concepts from the circular

This mapping follows the technical requirements in the supplied circular for **OOP's using JAVA, 25PCCSBS301**. A separate detailed semester syllabus was not supplied.

| Concept | Actual use | How to explain it in viva |
| --- | --- | --- |
| Classes and objects | `Student`, `AttendanceBook`, `AttendanceApp`, nested `Session` | A class is a plan; `new Student(...)` creates one student object. |
| Constructors | `Student(...)`, `Person(...)`, `AttendanceBook(Path)` | Constructors initialize objects. `super(name)` initializes inherited state. |
| Encapsulation | Private fields and getters; immutable students; copied collections | The UI cannot directly change the student list or stored marks. It must use validated methods. |
| Inheritance | `Student extends Person` | Student reuses Person's name and `getName()` method. |
| Abstraction | Abstract `Person` with abstract `description()` | Person describes a common contract but cannot itself be instantiated. |
| Runtime polymorphism | `Person person = student; person.description()` in search | The reference type is Person; the actual object is Student, so Java runs Student's override. |
| Interface | `Saveable`, implemented by `AttendanceBook` | The interface states that saving is supported; the class supplies file-writing behaviour. |
| Method overloading | Two `getStudents` methods with two and three parameters | Java chooses the matching parameter list at compile time. The third argument enables search. |
| Method overriding | `Student.description()`, `Session.toString()`, `save()`, Swing model methods | A child/implementing class supplies behaviour for an inherited contract. |
| Exception handling | `try/catch/finally`, `IOException`, `IllegalArgumentException` | Invalid input gets a useful error; failed writes roll back; temporary files are cleaned up. |
| Arrays | Demo names and table-column arrays | Arrays hold fixed sets such as the table's column titles. |
| Collections and generics | `List<Student>`, `ArrayList`, `Map<String, Boolean>`, `HashSet` | Lists store students; maps connect USNs to marks; sets detect duplicates. Generics constrain types. |
| String handling | `trim`, `toUpperCase`, `contains`, `split`, `matches` | Normalize input, search text and parse tab-separated records. |
| File handling | `Files.readAllLines`, `Files.write`, temporary files and `Files.move` | Persist the complete snapshot, then load it on restart. |
| Packages | `package attendance`, imports from `java.*` and `javax.swing.*` | Group related classes and access standard library classes by namespace. |
| `static` | `main`, validation helpers, shared `COURSES` | Class-level members belong to the class, not one instance. |
| `final` | Student fields, shared constants, final classes | A final field cannot be reassigned; a final class cannot be subclassed. Final references alone do not make collections immutable, so copies are used. |

Additional standard-Java concepts include `LocalDate`, lambdas for button actions, streams for counting/filtering, numeric sorting with `Comparator`, and Swing's event dispatch thread. They are used in small, localized places. The app has no custom checked exception: it uses standard Java exception types.

## Objects created during startup

`main` schedules work on the Swing event dispatch thread. It opens a file lock, constructs one `AttendanceBook` and one `AttendanceApp`, then shows a `JFrame`. First-run data creates 144 `Student` objects and 24 `Session` objects. Each session stores a copied list of immutable student objects and a separate copy of the attendance map. Screens create temporary buttons, labels, tables and panels when visited.

## A concrete polymorphism example

```java
Person person = student;
String text = person.description();
```

Although `person` is declared as `Person`, the object it refers to is a `Student`. At runtime Java calls `Student.description()`, which returns the student's name and USN. The search checks this text for a match. This is runtime polymorphism; it is different from the compile-time choice between overloaded `getStudents` methods.
