package com.kvs.student.repo;

import com.kvs.student.entity.StudentActivityCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentActivityCreditRepository extends JpaRepository<StudentActivityCredit, UUID> {
    List<StudentActivityCredit> findByStudentIdOrderByEventDateDesc(UUID studentId);
    List<StudentActivityCredit> findByHouseId(UUID houseId);

    @Query("SELECT COALESCE(SUM(c.pointsAwarded), 0) FROM StudentActivityCredit c WHERE c.houseId = :houseId")
    Integer sumPointsByHouseId(@Param("houseId") UUID houseId);
}
