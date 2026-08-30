package com.deliveryiq.routing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    Optional<Driver> findByEmployeeCode(String employeeCode);

    List<Driver> findByStatus(DriverStatus status);

    @Query("""
            SELECT d FROM Driver d
            WHERE d.status = com.deliveryiq.routing.DriverStatus.AVAILABLE
              AND d.homeDepot = :depot
            ORDER BY d.vehicleCapacityKg DESC
            """)
    List<Driver> findAvailableAtDepot(@Param("depot") String depot);

    long countByStatus(DriverStatus status);
}
