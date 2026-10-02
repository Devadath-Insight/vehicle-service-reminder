package com.vehicleservice.repository;

import com.vehicleservice.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByUserIdOrderByRegistrationNumberAsc(Long userId);

    /** Ownership-safe lookup: returns empty if the vehicle belongs to someone else. */
    Optional<Vehicle> findByIdAndUserId(Long id, Long userId);

    Optional<Vehicle> findByUserIdAndRegistrationNumberIgnoreCase(Long userId, String registrationNumber);

    long countByUserId(Long userId);

    @Query("""
           select v from Vehicle v
           where v.user.id = :userId
             and (:q is null
                  or lower(v.registrationNumber) like lower(concat('%', :q, '%'))
                  or lower(v.make) like lower(concat('%', :q, '%'))
                  or lower(v.model) like lower(concat('%', :q, '%'))
                  or lower(concat(concat(v.make, ' '), v.model)) like lower(concat('%', :q, '%')))
           order by v.registrationNumber asc
           """)
    List<Vehicle> search(@Param("userId") Long userId, @Param("q") String q);
}
