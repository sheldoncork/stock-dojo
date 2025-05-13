package cs309.stocks.classroom;

import cs309.stocks.Announcements.Announcement;
import cs309.stocks.Users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@NoArgsConstructor

public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int classroom_id;
    private String name;

    @Column(unique = true)  // Enforce uniqueness in the database
    private String joinCode;

    private Double startingAmount;


    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    // List of students associated with this classroom (many-to-many relationship)
    @ManyToMany(mappedBy = "studentClassrooms", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<User> students = new ArrayList<>();

    @OneToMany(mappedBy = "classroom")
    private List<Announcement> announcements = new ArrayList<>();


    public Classroom(String name, User teacher, Double startingAmount, String joinCode) {
        this.name = name;
        this.teacher = teacher;
        this.startingAmount = (startingAmount != null) ? startingAmount : 10000;
        this.joinCode = joinCode;
    }

    // Method to add a student to the classroom
    public void addStudent(User student) {
        if (!this.students.contains(student)) {
            this.students.add(student);
            student.getStudentClassrooms().add(this); // Update the student's list of classrooms
        }
    }

    // Remove a student from the classroom
    public void removeStudent(User student) {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null.");
        }

        if (!students.contains(student)) {
            throw new IllegalStateException("Student is not part of this classroom.");
        }

        // Ensure studentClassrooms is valid before proceeding
        if (student.getStudentClassrooms() == null) {
            throw new IllegalStateException("Student does not have a valid studentClassrooms list.");
        }

        if (!student.getStudentClassrooms().contains(this)) {
            throw new IllegalStateException("Classroom not found in student's list of classrooms.");
        }

        try {
            // Perform the removal only after all checks pass
            students.remove(student);
            student.getStudentClassrooms().remove(this);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while removing student from classroom: " + e.getMessage(), e);
        }
    }


}
