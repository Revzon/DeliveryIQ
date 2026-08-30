package com.deliveryiq.analytics;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AnalyticsAggregator analyticsAggregator;

    public DashboardController(DashboardService dashboardService, AnalyticsAggregator analyticsAggregator) {
        this.dashboardService = dashboardService;
        this.analyticsAggregator = analyticsAggregator;
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN', 'DISPATCHER')")
    public DashboardService.DashboardKpis kpis() {
        return dashboardService.kpis();
    }

    @GetMapping("/executive")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public Map<String, Object> executive(@RequestParam(required = false) String depot) {
        return dashboardService.executiveBundle(depot);
    }

    @GetMapping("/aggregate")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public DeliveryMetric aggregate(
            @RequestParam String depotCode,
            @RequestParam(required = false) java.time.LocalDate day) {
        return analyticsAggregator.aggregateForDepot(
                depotCode,
                day != null ? day : java.time.LocalDate.now()
        );
    }
}
