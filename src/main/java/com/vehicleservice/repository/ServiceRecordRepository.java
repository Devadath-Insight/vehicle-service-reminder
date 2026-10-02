package com.vehicleservice.repository;

import com.vehicleservice.model.ServiceRecord;
import com.vehicleservice.model.ServiceType;
import org.springframework.data.domain.Sort;
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
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {

    /** Ownership-safe lookup through vehicle -> user. */
    @EntityGraph(attributePaths = "vehicle")
    Optional<ServiceRecord> findByIdAndVehicleUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "vehicle")
    List<ServiceRecord> findTop5ByVehicleUserIdOrderByServiceDateDescIdDesc(Long userId);

    @EntityGraph(attributePaths = "vehicle")
    List<ServiceRecord> findByVehicleUserId(Long userId);

    long countByVehicleUserId(Long userId);

    @Query("select coalesce(sum(s.cost), 0) from ServiceRecord s where s.vehicle.user.id = :userId")
    BigDecimal sumCostByUserId(@Param("userId") Long userId);

    /** Search + filters. Every parameter except userId is optional (null = no filter). */
    @Query("""
           select s from ServiceRecord s join fetch s.vehicle v
           where v.user.id = :userId
             and (:vehicleId is null or v.id = :vehicleId)
             and (:serviceType is null or s.serviceType = :serviceType)
             and (:fromDate is null or s.serviceDate >= :fromDate)
             and (:toDate is null or s.serviceDate <= :toDate)
             and (:q is null
                  or lower(s.serviceCenter) like lower(concat('%', :q, '%'))
                  or lower(s.description) like lower(concat('%', :q, '%'))
                  or lower(v.registrationNumber) like lower(concat('%', :q, '%')))
           """)
    List<ServiceRecord> search(@Param("userId") Long userId,
                               @Param("vehicleId") Long vehicleId,
                               @Param("serviceType") ServiceType serviceType,
                               @Param("fromDate") LocalDate fromDate,
                               @Param("toDate") LocalDate toDate,
                               @Param("q") String q,
                               Sort sort);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ServiceRecord s where s.vehicle.id = :vehicleId")
    void deleteAllByVehicleId(@Param("vehicleId") Long vehicleId);
}
