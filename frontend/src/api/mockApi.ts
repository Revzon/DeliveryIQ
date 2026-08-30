import type {
  DashboardKpis,
  DriverSummary,
  RouteBoardItem,
  Shipment,
  TrendPoint,
} from '../types/delivery';

const delay = (ms = 350) => new Promise((resolve) => setTimeout(resolve, ms));

const mockShipments: Shipment[] = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    trackingNumber: 'DIQ-A1B2C3D4',
    status: 'IN_TRANSIT',
    priority: 'EXPRESS',
    originAddress: 'Kyiv Central Depot',
    destinationAddress: 'Lviv Warehouse 12',
    weightKg: 18.4,
    customerName: 'Nova Retail',
    customerRef: 'PO-88421',
    promisedDelivery: '2026-08-30T16:00:00Z',
    eta: '2026-08-30T15:20:00Z',
    stopSequence: 2,
    createdAt: '2026-08-29T08:00:00Z',
    updatedAt: '2026-08-30T09:15:00Z',
    timeline: [
      {
        id: 'e1',
        eventType: 'SHIPMENT_CREATED',
        status: 'CREATED',
        locationLabel: 'Kyiv Central Depot',
        occurredAt: '2026-08-29T08:00:00Z',
        recordedAt: '2026-08-29T08:00:01Z',
        source: 'SYSTEM',
      },
      {
        id: 'e2',
        eventType: 'STATUS_UPDATE',
        status: 'PICKED_UP',
        locationLabel: 'Dock B',
        occurredAt: '2026-08-29T10:20:00Z',
        recordedAt: '2026-08-29T10:20:04Z',
        source: 'SCANNER',
      },
      {
        id: 'e3',
        eventType: 'STATUS_UPDATE',
        status: 'IN_TRANSIT',
        locationLabel: 'M06 Corridor',
        occurredAt: '2026-08-30T07:40:00Z',
        recordedAt: '2026-08-30T07:40:12Z',
        source: 'DRIVER_APP',
        notes: 'Cleared weigh station',
      },
    ],
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    trackingNumber: 'DIQ-Z9Y8X7W6',
    status: 'DELAYED',
    priority: 'CRITICAL',
    originAddress: 'Odessa Hub',
    destinationAddress: 'Kharkiv Pharma',
    weightKg: 6.2,
    customerName: 'MedSupply UA',
    promisedDelivery: '2026-08-30T11:00:00Z',
    eta: '2026-08-30T14:45:00Z',
    createdAt: '2026-08-28T12:00:00Z',
    updatedAt: '2026-08-30T10:05:00Z',
    timeline: [
      {
        id: 'e4',
        eventType: 'SHIPMENT_CREATED',
        status: 'CREATED',
        locationLabel: 'Odessa Hub',
        occurredAt: '2026-08-28T12:00:00Z',
        recordedAt: '2026-08-28T12:00:02Z',
        source: 'SYSTEM',
      },
      {
        id: 'e5',
        eventType: 'STATUS_UPDATE',
        status: 'DELAYED',
        locationLabel: 'Checkpoint East',
        occurredAt: '2026-08-30T10:00:00Z',
        recordedAt: '2026-08-30T10:00:20Z',
        source: 'KAFKA',
        notes: 'Road closure — reroute pending',
      },
    ],
  },
  {
    id: '33333333-3333-3333-3333-333333333333',
    trackingNumber: 'DIQ-M4N5P6Q7',
    status: 'OUT_FOR_DELIVERY',
    priority: 'STANDARD',
    originAddress: 'Kyiv Central Depot',
    destinationAddress: 'Dnipro Storefront',
    weightKg: 22.0,
    customerName: 'Urban Goods',
    promisedDelivery: '2026-08-30T18:30:00Z',
    eta: '2026-08-30T17:50:00Z',
    stopSequence: 5,
    createdAt: '2026-08-29T14:00:00Z',
    updatedAt: '2026-08-30T11:00:00Z',
    timeline: [
      {
        id: 'e6',
        eventType: 'STATUS_UPDATE',
        status: 'OUT_FOR_DELIVERY',
        locationLabel: 'Dnipro Micro-hub',
        occurredAt: '2026-08-30T11:00:00Z',
        recordedAt: '2026-08-30T11:00:05Z',
        source: 'DRIVER_APP',
      },
    ],
  },
];

const mockRoutes: RouteBoardItem[] = [
  {
    id: 'r1',
    routeCode: 'RT-KYIV01',
    status: 'IN_PROGRESS',
    depotCode: 'KYIV-1',
    driverId: 'd1',
    driverName: 'Iryna Bondarenko',
    stopCount: 12,
    plannedStart: '2026-08-30T06:00:00Z',
    plannedEnd: '2026-08-30T15:30:00Z',
    plannedDistanceKm: 148.6,
    plannedDurationMin: 520,
    efficiencyScore: 91.2,
    etaSummary: 'On track · next ETA 15:20',
  },
  {
    id: 'r2',
    routeCode: 'RT-ODS07',
    status: 'ASSIGNED',
    depotCode: 'ODS-2',
    driverId: 'd2',
    driverName: 'Maksym Kravets',
    stopCount: 9,
    plannedStart: '2026-08-30T07:30:00Z',
    plannedEnd: '2026-08-30T16:00:00Z',
    plannedDistanceKm: 210.4,
    plannedDurationMin: 610,
    efficiencyScore: 84.5,
    etaSummary: 'Risk · weather delay +35m',
  },
  {
    id: 'r3',
    routeCode: 'RT-KYIV14',
    status: 'PLANNED',
    depotCode: 'KYIV-1',
    stopCount: 7,
    plannedStart: '2026-08-30T12:00:00Z',
    plannedEnd: '2026-08-30T18:00:00Z',
    plannedDistanceKm: 96.0,
    plannedDurationMin: 340,
    efficiencyScore: 88.0,
    etaSummary: 'Awaiting driver assignment',
  },
];

const mockDrivers: DriverSummary[] = [
  {
    id: 'd1',
    employeeCode: 'DRV-104',
    fullName: 'Iryna Bondarenko',
    status: 'ON_ROUTE',
    homeDepot: 'KYIV-1',
    vehicleCapacityKg: 1200,
  },
  {
    id: 'd2',
    employeeCode: 'DRV-221',
    fullName: 'Maksym Kravets',
    status: 'ON_ROUTE',
    homeDepot: 'ODS-2',
    vehicleCapacityKg: 1500,
  },
  {
    id: 'd3',
    employeeCode: 'DRV-088',
    fullName: 'Olena Shevchenko',
    status: 'AVAILABLE',
    homeDepot: 'KYIV-1',
    vehicleCapacityKg: 1000,
  },
];

const mockKpis: DashboardKpis = {
  onTimePercent: 94.2,
  routeEfficiency: 87.6,
  delayedCount: 18,
  criticalDelayedCount: 3,
  avgDelayMinutes: 41.5,
  activeRoutes: 26,
  deliveredToday: 412,
  availableDrivers: 14,
  generatedAt: new Date().toISOString(),
};

const mockTrend: TrendPoint[] = [
  { date: '2026-08-24', onTimePercent: 91.0, delayed: 22, efficiency: 84.0 },
  { date: '2026-08-25', onTimePercent: 92.4, delayed: 19, efficiency: 85.5 },
  { date: '2026-08-26', onTimePercent: 93.1, delayed: 17, efficiency: 86.2 },
  { date: '2026-08-27', onTimePercent: 90.8, delayed: 25, efficiency: 83.4 },
  { date: '2026-08-28', onTimePercent: 94.0, delayed: 16, efficiency: 87.1 },
  { date: '2026-08-29', onTimePercent: 95.2, delayed: 14, efficiency: 88.0 },
  { date: '2026-08-30', onTimePercent: 94.2, delayed: 18, efficiency: 87.6 },
];

export async function fetchShipments(statusFilter?: string): Promise<Shipment[]> {
  await delay();
  if (statusFilter && statusFilter !== 'ALL') {
    return mockShipments.filter((s) => s.status === statusFilter);
  }
  return mockShipments;
}

export async function fetchShipmentTimeline(trackingNumber: string): Promise<Shipment> {
  await delay();
  const found = mockShipments.find((s) => s.trackingNumber === trackingNumber);
  if (!found) {
    throw new Error(`Shipment ${trackingNumber} not found`);
  }
  return found;
}

export async function fetchRouteBoard(): Promise<{ routes: RouteBoardItem[]; drivers: DriverSummary[] }> {
  await delay(400);
  return { routes: mockRoutes, drivers: mockDrivers };
}

export async function fetchDashboard(): Promise<{ kpis: DashboardKpis; trend: TrendPoint[] }> {
  await delay(300);
  return { kpis: mockKpis, trend: mockTrend };
}
