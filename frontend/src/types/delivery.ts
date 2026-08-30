export type ShipmentStatus =
  | 'CREATED'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'DELAYED'
  | 'FAILED'
  | 'CANCELLED';

export type ShipmentPriority = 'STANDARD' | 'EXPRESS' | 'CRITICAL';

export type RouteStatus = 'PLANNED' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface TrackingEvent {
  id: string;
  eventType: string;
  status: string;
  locationLabel?: string;
  latitude?: number;
  longitude?: number;
  occurredAt: string;
  recordedAt: string;
  source: string;
  notes?: string;
}

export interface Shipment {
  id: string;
  trackingNumber: string;
  status: ShipmentStatus;
  priority: ShipmentPriority;
  originAddress: string;
  destinationAddress: string;
  weightKg: number;
  customerName: string;
  customerRef?: string;
  routeId?: string;
  driverId?: string;
  promisedDelivery: string;
  actualDelivery?: string;
  eta?: string;
  stopSequence?: number;
  createdAt: string;
  updatedAt: string;
  timeline: TrackingEvent[];
}

export interface RouteBoardItem {
  id: string;
  routeCode: string;
  status: RouteStatus;
  depotCode: string;
  driverId?: string;
  driverName?: string;
  stopCount: number;
  plannedStart?: string;
  plannedEnd?: string;
  plannedDistanceKm?: number;
  plannedDurationMin?: number;
  efficiencyScore?: number;
  etaSummary?: string;
}

export interface DriverSummary {
  id: string;
  employeeCode: string;
  fullName: string;
  status: 'AVAILABLE' | 'ON_ROUTE' | 'OFF_DUTY' | 'SUSPENDED';
  homeDepot: string;
  vehicleCapacityKg: number;
}

export interface DashboardKpis {
  onTimePercent: number;
  routeEfficiency: number;
  delayedCount: number;
  criticalDelayedCount: number;
  avgDelayMinutes: number;
  activeRoutes: number;
  deliveredToday: number;
  availableDrivers: number;
  generatedAt: string;
}

export interface TrendPoint {
  date: string;
  onTimePercent: number;
  delayed: number;
  efficiency: number;
}

export type LoadState = 'idle' | 'loading' | 'success' | 'error';
