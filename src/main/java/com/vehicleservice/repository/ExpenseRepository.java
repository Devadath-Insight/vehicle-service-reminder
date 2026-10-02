package com.vehicleservice.repository;

import com.vehicleservice.model.Expense;
import com.vehicleservice.model.ExpenseType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @EntityGraph(attributePaths = "vehicle")
    Optional<Expense> findByIdAndVehicleUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "vehicle")
    List<Expense> findTop5ByVehicleUserIdOrderByExpenseDateDescIdDesc(Long userId);

    @EntityGraph(attributePaths = "vehicle")
    List<Expense> findByVehicleUserId(Long userId);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.vehicle.user.id = :userId")
    BigDecimal sumAmountByUserId(@Param("userId") Long userId);

    @Query("""
           select e from Expense e join fetch e.vehicle v
           where v.user.id = :userId
             and (:vehicleId is null or v.id = :vehicleId)
             and (:expenseType is null or e.expenseType = :expenseType)
             and (:fromDate is null or e.expenseDate >= :fromDate)
             and (:toDate is null or e.expenseDate <= :toDate)
             and (:q is null
                  or lower(e.description) like lower(concat('%', :q, '%'))
                  or lower(v.registrationNumber) like lower(concat('%', :q, '%')))
           order by e.expenseDate desc, e.id desc
           """)
    List<Expense> search(@Param("userId") Long userId,
                         @Param("vehicleId") Long vehicleId,
                         @Param("expenseType") ExpenseType expenseType,
                         @Param("fromDate") LocalDate fromDate,
                         @Param("toDate") LocalDate toDate,
                         @Param("q") String q);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Expense e where e.vehicle.id = :vehicleId")
    void deleteAllByVehicleId(@Param("vehicleId") Long vehicleId);
}
