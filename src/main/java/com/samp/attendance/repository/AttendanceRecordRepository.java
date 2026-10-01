package com.samp.attendance.repository;

import com.samp.attendance.domain.AttendanceRecord;
import com.samp.attendance.domain.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    /** Existing records for a session, so reopening the screen edits instead of duplicating (AC-06.3). */
    List<AttendanceRecord> findByCourseAndSessionDate(Course course, LocalDate sessionDate);

    /**
     * Paged listing for the records screen (AC-07.1, AC-07.2).
     *
     * <p>{@code updatedBy} is fetched eagerly here on purpose. The view renders it,
     * and with {@code spring.jpa.open-in-view=false} the persistence context is
     * already closed by the time Thymeleaf runs, so a lazy proxy would throw
     * LazyInitializationException. A left join keeps records whose updatedBy is
     * null. Fetch-joining a to-one association is safe with pagination; a to-many
     * would not be, which is why only this association is joined.
     */
    @Query(value = "select r from AttendanceRecord r "
                 + "left join fetch r.updatedBy "
                 + "order by r.sessionDate desc, r.student.rollNumber asc",
           countQuery = "select count(r) from AttendanceRecord r")
    Page<AttendanceRecord> findAllNewestFirst(Pageable pageable);
}
