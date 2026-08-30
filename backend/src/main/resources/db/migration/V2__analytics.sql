-- DeliveryIQ analytics schema: delivery metrics and dashboard snapshots
-- Author: Pavlo Kislov

CREATE TABLE delivery_metrics (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_date         DATE         NOT NULL,
    depot_code          VARCHAR(64)  NOT NULL,
    total_shipments     INTEGER      NOT NULL DEFAULT 0,
    delivered_on_time   INTEGER      NOT NULL DEFAULT 0,
    delivered_late      INTEGER      NOT NULL DEFAULT 0,
    failed_deliveries   INTEGER      NOT NULL DEFAULT 0,
    avg_delay_minutes   NUMERIC(10, 2),
    avg_route_efficiency NUMERIC(5, 2),
    total_distance_km   NUMERIC(12, 2),
    active_drivers      INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_delivery_metrics_day_depot UNIQUE (metric_date, depot_code)
);

CREATE TABLE dashboard_snapshots (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_key        VARCHAR(64)  NOT NULL UNIQUE,
    window_start        TIMESTAMPTZ  NOT NULL,
    window_end          TIMESTAMPTZ  NOT NULL,
    on_time_percent     NUMERIC(5, 2) NOT NULL,
    route_efficiency    NUMERIC(5, 2) NOT NULL,
    delayed_count       INTEGER      NOT NULL DEFAULT 0,
    in_transit_count    INTEGER      NOT NULL DEFAULT 0,
    delivered_today     INTEGER      NOT NULL DEFAULT 0,
    active_routes       INTEGER      NOT NULL DEFAULT 0,
    available_drivers   INTEGER      NOT NULL DEFAULT 0,
    payload_json        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_delivery_metrics_date ON delivery_metrics(metric_date DESC);
CREATE INDEX idx_delivery_metrics_depot ON delivery_metrics(depot_code, metric_date DESC);
CREATE INDEX idx_dashboard_snapshots_window ON dashboard_snapshots(window_start, window_end);
CREATE INDEX idx_dashboard_snapshots_created ON dashboard_snapshots(created_at DESC);
