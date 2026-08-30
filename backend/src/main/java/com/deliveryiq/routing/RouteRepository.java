package com.deliveryiq.routing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteRepository extends JpaRepository<Route, UUID> {

    Optional<Route> findByRouteCode(String routeCode);

    List<Route> findByStatus(RouteStatus status);

    @Query("""
            SELECT r FROM Route r
            WHERE r.efficiencyScore IS NOT NULL
              AND r.efficiencyScore >= :minScore
            ORDER BY r.efficiencyScore DESC
            """)
    List<Route> findEfficientRoutes(@Param("minScore") BigDecimal minScore);

    @Query("""
            SELECT r FROM Route r
            WHERE r.driver.id = :driverId
              AND r.status IN :statuses
              AND r.plannedStart < :end
              AND r.plannedEnd > :start
            """)
    List<Route> findConflicts(
            @Param("driverId") UUID driverId,
            @Param("statuses") List<RouteStatus> statuses,
            @Param("start") Instant start,
            @Param("end") Instant end);

    long countByStatusIn(List<RouteStatus> statuses);
}
