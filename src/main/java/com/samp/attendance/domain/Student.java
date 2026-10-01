package com.samp.attendance.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "student")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "roll_number", nullable = false, unique = true, length = 30)
    private String rollNumber;

    @NotBlank
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Links a student to their login so US-10 can scope the view to them. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id")
    private AppUser appUser;

    protected Student() { }

    public Student(String rollNumber, String fullName, AppUser appUser) {
        this.rollNumber = rollNumber;
        this.fullName = fullName;
        this.appUser = appUser;
    }

    public Long getId() { return id; }
    public String getRollNumber() { return rollNumber; }
    public String getFullName() { return fullName; }
    public AppUser getAppUser() { return appUser; }
}
