package com.samp.attendance.domain;

import jakarta.persistence.*;

/** Which students are in which course; drives the entry-screen roster (AC-06.1). */
@Entity
@Table(name = "enrolment",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
public class Enrolment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    protected Enrolment() { }

    public Enrolment(Student student, Course course) {
        this.student = student;
        this.course = course;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
}
