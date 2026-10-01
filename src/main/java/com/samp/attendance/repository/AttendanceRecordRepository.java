package com.samp.attendance.repository;

import com.samp.attendance.domain.AttendanceRecord;
import com.samp.attendance.domain.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.samp.attendance.domain.WorkflowState;

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

    /**
     * Search with every filter optional and combinable (US-09).
     *
     * <p>Written as one query with null-guarded predicates rather than four code
     * paths, so that any subset of filters composes without a combinatorial
     * explosion of methods.
     */
    @Query(value = "select r from AttendanceRecord r "
                 + "left join fetch r.updatedBy "
                 + "where (:rollNumber is null or lower(r.student.rollNumber) like lower(concat('%', :rollNumber, '%'))) "
                 + "and   (:courseId   is null or r.course.id = :courseId) "
                 + "and   (:from       is null or r.sessionDate >= :from) "
                 + "and   (:to         is null or r.sessionDate <= :to) "
                 + "and   (:state      is null or r.workflowState = :state) "
                 + "order by r.sessionDate desc, r.student.rollNumber asc",
           countQuery = "select count(r) from AttendanceRecord r "
                 + "where (:rollNumber is null or lower(r.student.rollNumber) like lower(concat('%', :rollNumber, '%'))) "
                 + "and   (:courseId   is null or r.course.id = :courseId) "
                 + "and   (:from       is null or r.sessionDate >= :from) "
                 + "and   (:to         is null or r.sessionDate <= :to) "
                 + "and   (:state      is null or r.workflowState = :state)")
    Page<AttendanceRecord> search(@Param("rollNumber") String rollNumber,
                                  @Param("courseId") Long courseId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to,
                                  @Param("state") WorkflowState state,
                                  Pageable pageable);

    /** Records awaiting ADMIN action, for the pending queue (US-16). */
    long countByWorkflowState(WorkflowState state);

    /** Approved records only: an unverified draft must never move the official number. */
    @Query("select r from AttendanceRecord r "
         + "where r.workflowState = com.samp.attendance.domain.WorkflowState.APPROVED "
         + "order by r.student.rollNumber, r.course.code")
    List<AttendanceRecord> findApproved();

    /** A single student's records, for the student view (US-10). */
    @Query("select r from AttendanceRecord r left join fetch r.updatedBy "
         + "where r.student.rollNumber = :rollNumber "
         + "order by r.sessionDate desc")
    List<AttendanceRecord> findByStudentRollNumber(@Param("rollNumber") String rollNumber);
}
