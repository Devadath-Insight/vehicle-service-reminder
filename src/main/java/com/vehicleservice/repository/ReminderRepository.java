package com.vehicleservice.repository;

import com.vehicleservice.model.Reminder;
import com.vehicleservice.model.ReminderStatus;
import com.vehicleservice.model.ReminderType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    @EntityGraph(attributePaths = "vehicle")
    Optional<Reminder> findByIdAndVehicleUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "vehicle")
    List<Reminder> findByVehicleUserId(Long userId);

    @Query("""
           select r from Reminder r join fetch r.vehicle v
           where v.user.id = :userId
             and (:vehicleId is null or v.id = :vehicleId)
             and (:status is null or r.status = :status)
             and (:reminderType is null or r.reminderType = :reminderType)
           """)
    List<Reminder> search(@Param("userId") Long userId,
                          @Param("vehicleId") Long vehicleId,
                          @Param("status") ReminderStatus status,
                          @Param("reminderType") ReminderType reminderType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Reminder r where r.vehicle.id = :vehicleId")
    void deleteAllByVehicleId(@Param("vehicleId") Long vehicleId);
}
