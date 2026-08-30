import { useEffect, useMemo, useState } from 'react';
import { fetchRouteBoard } from '../api/mockApi';
import type { DriverSummary, LoadState, RouteBoardItem, RouteStatus } from '../types/delivery';

export function RouteBoard() {
  const [routes, setRoutes] = useState<RouteBoardItem[]>([]);
  const [drivers, setDrivers] = useState<DriverSummary[]>([]);
  const [status, setStatus] = useState<'ALL' | RouteStatus>('ALL');
  const [depot, setDepot] = useState('ALL');
  const [loadState, setLoadState] = useState<LoadState>('idle');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoadState('loading');
    fetchRouteBoard()
      .then((data) => {
        if (cancelled) return;
        setRoutes(data.routes);
        setDrivers(data.drivers);
        setLoadState('success');
      })
      .catch((err: Error) => {
        if (cancelled) return;
        setError(err.message);
        setLoadState('error');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const depots = useMemo(
    () => ['ALL', ...Array.from(new Set(routes.map((r) => r.depotCode)))],
    [routes],
  );

  const filtered = useMemo(() => {
    return routes.filter((r) => {
      const statusOk = status === 'ALL' || r.status === status;
      const depotOk = depot === 'ALL' || r.depotCode === depot;
      return statusOk && depotOk;
    });
  }, [routes, status, depot]);

  return (
    <section className="page route-board-page">
      <header className="page-header">
        <h1>Route board</h1>
        <p>Planned and active routes with driver assignment and ETA risk signals.</p>
      </header>

      <div className="toolbar">
        <label>
          Route status
          <select value={status} onChange={(e) => setStatus(e.target.value as typeof status)}>
            <option value="ALL">ALL</option>
            <option value="PLANNED">PLANNED</option>
            <option value="ASSIGNED">ASSIGNED</option>
            <option value="IN_PROGRESS">IN_PROGRESS</option>
            <option value="COMPLETED">COMPLETED</option>
          </select>
        </label>
        <label>
          Depot
          <select value={depot} onChange={(e) => setDepot(e.target.value)}>
            {depots.map((d) => (
              <option key={d} value={d}>{d}</option>
            ))}
          </select>
        </label>
      </div>

      {loadState === 'loading' && <div className="state-banner">Loading routes…</div>}
      {loadState === 'error' && <div className="state-banner error" role="alert">{error}</div>}

      <div className="route-layout">
        <div className="route-table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Route</th>
                <th>Status</th>
                <th>Depot</th>
                <th>Driver</th>
                <th>Stops</th>
                <th>Distance</th>
                <th>Duration</th>
                <th>Efficiency</th>
                <th>ETA</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((route) => (
                <tr key={route.id}>
                  <td>{route.routeCode}</td>
                  <td><span className={`badge status-${route.status.toLowerCase()}`}>{route.status}</span></td>
                  <td>{route.depotCode}</td>
                  <td>{route.driverName ?? 'Unassigned'}</td>
                  <td>{route.stopCount}</td>
                  <td>{route.plannedDistanceKm?.toFixed(1) ?? '—'} km</td>
                  <td>{route.plannedDurationMin ?? '—'} min</td>
                  <td>{route.efficiencyScore?.toFixed(1) ?? '—'}%</td>
                  <td>{route.etaSummary ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {loadState === 'success' && filtered.length === 0 && (
            <p className="empty">No routes for the selected filters.</p>
          )}
        </div>

        <aside className="driver-panel">
          <h2>Drivers</h2>
          <ul>
            {drivers.map((driver) => (
              <li key={driver.id}>
                <strong>{driver.fullName}</strong>
                <span className={`badge status-${driver.status.toLowerCase()}`}>{driver.status}</span>
                <p className="muted">{driver.employeeCode} · {driver.homeDepot}</p>
                <p className="muted">Capacity {driver.vehicleCapacityKg} kg</p>
              </li>
            ))}
          </ul>
        </aside>
      </div>
    </section>
  );
}
