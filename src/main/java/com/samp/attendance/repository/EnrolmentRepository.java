package com.samp.attendance.repository;

import com.samp.attendance.domain.Course;
import com.samp.attendance.domain.Enrolment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface EnrolmentRepository extends JpaRepository<Enrolment, Long> {

    /** Roster for the entry screen, ordered by roll number so the list is stable (AC-06.1). */
    @Query("select e from Enrolment e join fetch e.student s where e.course = :course order by s.rollNumber")
    List<Enrolment> findRosterForCourse(Course course);
}
