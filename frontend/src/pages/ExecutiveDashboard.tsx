import { useEffect, useState } from 'react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { fetchDashboard } from '../api/mockApi';
import type { DashboardKpis, LoadState, TrendPoint } from '../types/delivery';

function KpiWidget({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="kpi-widget">
      <span className="kpi-label">{label}</span>
      <strong className="kpi-value">{value}</strong>
      {hint && <span className="kpi-hint">{hint}</span>}
    </div>
  );
}

export function ExecutiveDashboard() {
  const [kpis, setKpis] = useState<DashboardKpis | null>(null);
  const [trend, setTrend] = useState<TrendPoint[]>([]);
  const [loadState, setLoadState] = useState<LoadState>('idle');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoadState('loading');
    fetchDashboard()
      .then((data) => {
        if (cancelled) return;
        setKpis(data.kpis);
        setTrend(data.trend);
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

  return (
    <section className="page dashboard-page">
      <header className="page-header">
        <h1>Executive dashboard</h1>
        <p>On-time performance, route efficiency, and delayed delivery pressure.</p>
      </header>

      {loadState === 'loading' && <div className="state-banner">Refreshing KPI snapshot…</div>}
      {loadState === 'error' && <div className="state-banner error" role="alert">{error}</div>}

      {kpis && (
        <>
          <div className="kpi-grid">
            <KpiWidget label="On-time %" value={`${kpis.onTimePercent.toFixed(1)}%`} hint="Last 7 days" />
            <KpiWidget label="Route efficiency" value={`${kpis.routeEfficiency.toFixed(1)}%`} />
            <KpiWidget
              label="Delayed shipments"
              value={String(kpis.delayedCount)}
              hint={`${kpis.criticalDelayedCount} critical · avg ${kpis.avgDelayMinutes} min`}
            />
            <KpiWidget label="Active routes" value={String(kpis.activeRoutes)} />
            <KpiWidget label="Delivered today" value={String(kpis.deliveredToday)} />
            <KpiWidget label="Available drivers" value={String(kpis.availableDrivers)} />
          </div>

          <div className="chart-panel">
            <h2>On-time & efficiency trend</h2>
            <div className="chart-frame">
              <ResponsiveContainer width="100%" height={280}>
                <AreaChart data={trend}>
                  <defs>
                    <linearGradient id="onTimeFill" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#1f6f5b" stopOpacity={0.35} />
                      <stop offset="95%" stopColor="#1f6f5b" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#d5ddd8" />
                  <XAxis dataKey="date" tickFormatter={(v) => String(v).slice(5)} />
                  <YAxis domain={[80, 100]} />
                  <Tooltip />
                  <Area type="monotone" dataKey="onTimePercent" stroke="#1f6f5b" fill="url(#onTimeFill)" name="On-time %" />
                  <Area type="monotone" dataKey="efficiency" stroke="#c45c26" fillOpacity={0} name="Efficiency %" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
            <p className="muted">Snapshot generated {new Date(kpis.generatedAt).toLocaleString()}</p>
          </div>

          <div className="delayed-panel">
            <h2>Delayed metrics</h2>
            <ul>
              <li>Open delayed shipments: <strong>{kpis.delayedCount}</strong></li>
              <li>Critical delayed: <strong>{kpis.criticalDelayedCount}</strong></li>
              <li>Average delay: <strong>{kpis.avgDelayMinutes} minutes</strong></li>
            </ul>
          </div>
        </>
      )}
    </section>
  );
}
