package com.deliveryiq.analytics;

import com.deliveryiq.cache.CacheService;
import com.deliveryiq.routing.DriverRepository;
import com.deliveryiq.routing.DriverStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final KpiService kpiService;
    private final AnalyticsAggregator analyticsAggregator;
    private final DriverRepository driverRepository;
    private final CacheService cacheService;

    public DashboardService(
            KpiService kpiService,
            AnalyticsAggregator analyticsAggregator,
            DriverRepository driverRepository,
            CacheService cacheService) {
        this.kpiService = kpiService;
        this.analyticsAggregator = analyticsAggregator;
        this.driverRepository = driverRepository;
        this.cacheService = cacheService;
    }

    public DashboardKpis kpis() {
        return cacheService.getOrLoad("dashboard:kpis", DashboardKpis.class, this::buildKpis, cacheService.dashboardTtlSeconds());
    }

    public Map<String, Object> executiveBundle(String depot) {
        DashboardKpis kpis = kpis();
        LocalDate from = LocalDate.now().minus(14, ChronoUnit.DAYS);
        Map<String, Object> trend = analyticsAggregator.trend(from, depot);
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("kpis", kpis);
        bundle.put("trend", trend);
        bundle.put("generatedAt", Instant.now().toString());
        return bundle;
    }

    private DashboardKpis buildKpis() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(7);
        KpiService.DelayedStats delayed = kpiService.delayedStats();
        return new DashboardKpis(
                kpiService.onTimePercent(from, to),
                kpiService.routeEfficiency(),
                delayed.delayedCount(),
                delayed.criticalCount(),
                delayed.avgDelayMinutes(),
                kpiService.activeRoutes(),
                kpiService.deliveredToday(),
                driverRepository.countByStatus(DriverStatus.AVAILABLE),
                Instant.now()
        );
    }

    public record DashboardKpis(
            BigDecimal onTimePercent,
            BigDecimal routeEfficiency,
            long delayedCount,
            long criticalDelayedCount,
            BigDecimal avgDelayMinutes,
            long activeRoutes,
            long deliveredToday,
            long availableDrivers,
            Instant generatedAt
    ) {
    }
}
