-- DeliveryIQ baseline schema: shipments, tracking, drivers, routes
-- Author: Pavlo Kislov / Oleksandr Bekshaiev

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE drivers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_code   VARCHAR(32)  NOT NULL UNIQUE,
    full_name       VARCHAR(128) NOT NULL,
    phone           VARCHAR(32),
    email           VARCHAR(128),
    status          VARCHAR(32)  NOT NULL DEFAULT 'AVAILABLE',
    home_depot      VARCHAR(64)  NOT NULL,
    vehicle_capacity_kg NUMERIC(10, 2) NOT NULL DEFAULT 1000.00,
    max_stops       INTEGER      NOT NULL DEFAULT 25,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_driver_status CHECK (status IN ('AVAILABLE', 'ON_ROUTE', 'OFF_DUTY', 'SUSPENDED'))
);

CREATE TABLE routes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_code          VARCHAR(32)  NOT NULL UNIQUE,
    driver_id           UUID         REFERENCES drivers(id),
    status              VARCHAR(32)  NOT NULL DEFAULT 'PLANNED',
    depot_code          VARCHAR(64)  NOT NULL,
    planned_distance_km NUMERIC(10, 2),
    planned_duration_min INTEGER,
    efficiency_score    NUMERIC(5, 2),
    planned_start       TIMESTAMPTZ,
    planned_end         TIMESTAMPTZ,
    actual_start        TIMESTAMPTZ,
    actual_end          TIMESTAMPTZ,
    stop_count          INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_route_status CHECK (status IN ('PLANNED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE shipments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tracking_number     VARCHAR(48)  NOT NULL UNIQUE,
    status              VARCHAR(32)  NOT NULL DEFAULT 'CREATED',
    priority            VARCHAR(16)  NOT NULL DEFAULT 'STANDARD',
    origin_address      VARCHAR(256) NOT NULL,
    destination_address VARCHAR(256) NOT NULL,
    origin_lat          NUMERIC(10, 7),
    origin_lng          NUMERIC(10, 7),
    destination_lat     NUMERIC(10, 7),
    destination_lng     NUMERIC(10, 7),
    weight_kg           NUMERIC(10, 2) NOT NULL,
    volume_m3           NUMERIC(10, 3),
    customer_name       VARCHAR(128) NOT NULL,
    customer_ref        VARCHAR(64),
    route_id            UUID         REFERENCES routes(id),
    driver_id           UUID         REFERENCES drivers(id),
    promised_delivery   TIMESTAMPTZ  NOT NULL,
    actual_delivery     TIMESTAMPTZ,
    eta                 TIMESTAMPTZ,
    stop_sequence       INTEGER,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_shipment_status CHECK (status IN (
        'CREATED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY',
        'DELIVERED', 'DELAYED', 'FAILED', 'CANCELLED'
    )),
    CONSTRAINT chk_shipment_priority CHECK (priority IN ('STANDARD', 'EXPRESS', 'CRITICAL'))
);

CREATE TABLE tracking_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id     UUID         NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    event_type      VARCHAR(48)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    location_label  VARCHAR(256),
    latitude        NUMERIC(10, 7),
    longitude       NUMERIC(10, 7),
    occurred_at     TIMESTAMPTZ  NOT NULL,
    recorded_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    source          VARCHAR(32)  NOT NULL DEFAULT 'SYSTEM',
    notes           TEXT,
    CONSTRAINT chk_tracking_source CHECK (source IN ('SYSTEM', 'DRIVER_APP', 'SCANNER', 'CUSTOMER', 'KAFKA'))
);

CREATE INDEX idx_shipments_status ON shipments(status);
CREATE INDEX idx_shipments_promised ON shipments(promised_delivery);
CREATE INDEX idx_shipments_route ON shipments(route_id);
CREATE INDEX idx_shipments_driver ON shipments(driver_id);
CREATE INDEX idx_shipments_tracking_number ON shipments(tracking_number);
CREATE INDEX idx_tracking_events_shipment ON tracking_events(shipment_id, occurred_at DESC);
CREATE INDEX idx_tracking_events_occurred ON tracking_events(occurred_at);
CREATE INDEX idx_routes_status ON routes(status);
CREATE INDEX idx_routes_driver ON routes(driver_id);
CREATE INDEX idx_drivers_status ON drivers(status);
CREATE INDEX idx_drivers_depot ON drivers(home_depot);
