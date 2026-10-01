package com.samp.attendance.config;

import com.samp.attendance.domain.*;
import com.samp.attendance.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * Seeds demo data on first start.
 *
 * <p>Fixed, known data matters beyond convenience: the Selenium suite asserts
 * against these exact roll numbers and course codes, so a stable seed is what
 * keeps those tests deterministic (risk R1). No real student data is used (C13).
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(AppUserRepository users, CourseRepository courses,
                           StudentRepository students, EnrolmentRepository enrolments,
                           PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return; // already seeded
            }

            users.save(new AppUser("admin",
                    encoder.encode("admin123"), "Dr. Anita Rao (HoD)", Role.ADMIN));
            users.save(new AppUser("faculty1",
                    encoder.encode("faculty123"), "Prof. Suresh Kumar", Role.FACULTY));
            users.save(new AppUser("faculty2",
                    encoder.encode("faculty123"), "Prof. Meera Nair", Role.FACULTY));

            Course cs301 = courses.save(new Course("CS301", "Software Engineering", 40));
            Course cs302 = courses.save(new Course("CS302", "Database Systems", 36));

            record Seed(String roll, String name) { }
            List<Seed> seeds = List.of(
                    new Seed("22CS001", "Aarav Sharma"),
                    new Seed("22CS002", "Diya Patel"),
                    new Seed("22CS003", "Rohan Gupta"),
                    new Seed("22CS004", "Ananya Singh"),
                    new Seed("22CS005", "Vihaan Reddy"),
                    new Seed("22CS006", "Ishita Joshi"),
                    new Seed("22CS007", "Arjun Menon"),
                    new Seed("22CS008", "Saanvi Iyer"));

            for (Seed s : seeds) {
                AppUser login = users.save(new AppUser(
                        s.roll().toLowerCase(), encoder.encode("student123"), s.name(), Role.STUDENT));
                Student student = students.save(new Student(s.roll(), s.name(), login));
                enrolments.save(new Enrolment(student, cs301));
                // Only the first five also take CS302, so the dashboard has varied data.
                if (seeds.indexOf(s) < 5) {
                    enrolments.save(new Enrolment(student, cs302));
                }
            }
        };
    }
}
