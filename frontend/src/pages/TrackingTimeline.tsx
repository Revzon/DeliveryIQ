import { useEffect, useMemo, useState } from 'react';
import { fetchShipmentTimeline, fetchShipments } from '../api/mockApi';
import type { LoadState, Shipment, ShipmentStatus, TrackingEvent } from '../types/delivery';

const STATUS_OPTIONS: Array<'ALL' | ShipmentStatus> = [
  'ALL',
  'CREATED',
  'PICKED_UP',
  'IN_TRANSIT',
  'OUT_FOR_DELIVERY',
  'DELAYED',
  'DELIVERED',
];

export function TrackingTimeline() {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [selected, setSelected] = useState<Shipment | null>(null);
  const [statusFilter, setStatusFilter] = useState<'ALL' | ShipmentStatus>('ALL');
  const [eventFilter, setEventFilter] = useState('ALL');
  const [trackingQuery, setTrackingQuery] = useState('');
  const [loadState, setLoadState] = useState<LoadState>('idle');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoadState('loading');
    setError(null);
    fetchShipments(statusFilter)
      .then((data) => {
        if (cancelled) return;
        setShipments(data);
        setSelected((prev) => prev ?? data[0] ?? null);
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
  }, [statusFilter]);

  const events: TrackingEvent[] = useMemo(() => {
    if (!selected) return [];
    if (eventFilter === 'ALL') return selected.timeline;
    return selected.timeline.filter((e) => e.eventType === eventFilter || e.status === eventFilter);
  }, [selected, eventFilter]);

  async function lookupTracking() {
    if (!trackingQuery.trim()) return;
    setLoadState('loading');
    setError(null);
    try {
      const shipment = await fetchShipmentTimeline(trackingQuery.trim().toUpperCase());
      setSelected(shipment);
      setShipments((prev) => {
        const exists = prev.some((s) => s.id === shipment.id);
        return exists ? prev : [shipment, ...prev];
      });
      setLoadState('success');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lookup failed');
      setLoadState('error');
    }
  }

  return (
    <section className="page tracking-page">
      <header className="page-header">
        <h1>Shipment tracking</h1>
        <p>Live history, status transitions, and event filtering for active deliveries.</p>
      </header>

      <div className="toolbar">
        <label>
          Status
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as typeof statusFilter)}>
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </label>
        <label>
          Event type
          <select value={eventFilter} onChange={(e) => setEventFilter(e.target.value)}>
            <option value="ALL">ALL</option>
            <option value="SHIPMENT_CREATED">SHIPMENT_CREATED</option>
            <option value="STATUS_UPDATE">STATUS_UPDATE</option>
            <option value="DELAYED">DELAYED</option>
          </select>
        </label>
        <div className="lookup">
          <input
            value={trackingQuery}
            onChange={(e) => setTrackingQuery(e.target.value)}
            placeholder="Tracking number"
            aria-label="Tracking number"
          />
          <button type="button" onClick={lookupTracking}>Track</button>
        </div>
      </div>

      {loadState === 'loading' && <div className="state-banner">Loading tracking data…</div>}
      {loadState === 'error' && <div className="state-banner error" role="alert">{error}</div>}

      <div className="tracking-layout">
        <aside className="shipment-list" aria-label="Shipments">
          {shipments.map((s) => (
            <button
              key={s.id}
              type="button"
              className={selected?.id === s.id ? 'shipment-item active' : 'shipment-item'}
              onClick={() => setSelected(s)}
            >
              <strong>{s.trackingNumber}</strong>
              <span className={`badge status-${s.status.toLowerCase()}`}>{s.status}</span>
              <span>{s.customerName}</span>
            </button>
          ))}
          {loadState === 'success' && shipments.length === 0 && (
            <p className="empty">No shipments match this filter.</p>
          )}
        </aside>

        <div className="timeline-panel">
          {selected ? (
            <>
              <div className="timeline-meta">
                <h2>{selected.trackingNumber}</h2>
                <p>{selected.originAddress} → {selected.destinationAddress}</p>
                <p>Promised {new Date(selected.promisedDelivery).toLocaleString()} · ETA {selected.eta ? new Date(selected.eta).toLocaleString() : 'n/a'}</p>
              </div>
              <ol className="timeline">
                {events.map((event) => (
                  <li key={event.id}>
                    <div className="timeline-dot" />
                    <div>
                      <strong>{event.eventType}</strong>
                      <span className="muted"> · {event.status}</span>
                      <p>{event.locationLabel ?? 'Unknown location'}</p>
                      <p className="muted">{new Date(event.occurredAt).toLocaleString()} · {event.source}</p>
                      {event.notes && <p className="notes">{event.notes}</p>}
                    </div>
                  </li>
                ))}
                {events.length === 0 && <li className="empty">No events for this filter.</li>}
              </ol>
            </>
          ) : (
            <p className="empty">Select a shipment to inspect its timeline.</p>
          )}
        </div>
      </div>
    </section>
  );
}
