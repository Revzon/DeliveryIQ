import React from 'react';
import { createRoot } from 'react-dom/client';
import { ExecutiveDashboard } from './pages/ExecutiveDashboard';
import { RouteBoard } from './pages/RouteBoard';
import { TrackingTimeline } from './pages/TrackingTimeline';
import './styles/responsive.css';

type View = 'tracking' | 'routes' | 'dashboard';

function App() {
  const [view, setView] = React.useState<View>('dashboard');

  return (
    <div className="app-shell">
      <nav className="app-nav">
        <strong className="brand">DeliveryIQ</strong>
        <button type="button" className={view === 'dashboard' ? 'active' : ''} onClick={() => setView('dashboard')}>Dashboard</button>
        <button type="button" className={view === 'tracking' ? 'active' : ''} onClick={() => setView('tracking')}>Tracking</button>
        <button type="button" className={view === 'routes' ? 'active' : ''} onClick={() => setView('routes')}>Routes</button>
      </nav>
      {view === 'dashboard' && <ExecutiveDashboard />}
      {view === 'tracking' && <TrackingTimeline />}
      {view === 'routes' && <RouteBoard />}
    </div>
  );
}

const root = document.getElementById('root');
if (root) {
  createRoot(root).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  );
}
