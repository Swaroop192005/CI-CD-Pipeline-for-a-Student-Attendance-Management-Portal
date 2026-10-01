package com.samp.attendance.repository;

import com.samp.attendance.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByRollNumber(String rollNumber);
    Optional<Student> findByAppUserUsername(String username);
    boolean existsByRollNumber(String rollNumber);
}
