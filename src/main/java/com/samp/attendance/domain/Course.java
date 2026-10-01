package com.samp.attendance.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String title;

    /** Denominator context for reporting; percentages use approved records (§4.2). */
    @Min(0)
    @Column(name = "total_sessions", nullable = false)
    private int totalSessions;

    protected Course() { }

    public Course(String code, String title, int totalSessions) {
        this.code = code;
        this.title = title;
        this.totalSessions = totalSessions;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public int getTotalSessions() { return totalSessions; }

    public String getDisplayName() { return code + " — " + title; }
}
