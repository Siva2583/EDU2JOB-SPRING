import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Chart as ChartJS, CategoryScale, LinearScale, BarElement, PointElement,
  LineElement, Title, Tooltip, Legend, ArcElement,
} from 'chart.js';
import { Bar, Doughnut, Line } from 'react-chartjs-2';
import api, { apiErrorMessage } from '../api';
import '../App.css';

ChartJS.register(CategoryScale, LinearScale, BarElement, PointElement,
  LineElement, Title, Tooltip, Legend, ArcElement);

const LEGACY_LABEL = 'Legacy (previous model)';

function Archive() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedId, setExpandedId] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    (async () => {
      try {
        const res = await api.get('/api/predict/history');
        setHistory(Array.isArray(res.data) ? res.data : []);
      } catch (err) {
        setError(apiErrorMessage(err, 'Could not load your prediction history.'));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  // All analytics below are computed from the user's real persisted
  // history only — nothing is invented to fill a chart.
  const stats = useMemo(() => {
    const roleCounts = {};
    const skillCounts = {};
    const byMonth = {};
    let scoredCount = 0;
    let scoreSum = 0;

    history.forEach((item) => {
      const preds = Array.isArray(item.predictions) ? item.predictions : [];
      preds.forEach((p) => {
        if (p && p.role) roleCounts[p.role] = (roleCounts[p.role] || 0) + 1;
      });
      if (preds.length > 0 && typeof preds[0].score === 'number') {
        const s = preds[0].score <= 1 ? preds[0].score * 100 : preds[0].score;
        scoreSum += s;
        scoredCount += 1;
      }
      const skills = Array.isArray(item.skills)
        ? item.skills
        : String(item.skills || '').split(',').map((s) => s.trim()).filter(Boolean);
      skills.forEach((s) => { skillCounts[s] = (skillCounts[s] || 0) + 1; });
      if (item.created_at) {
        const month = String(item.created_at).slice(0, 7);
        byMonth[month] = (byMonth[month] || 0) + 1;
      }
    });

    const topSkills = Object.entries(skillCounts).sort((a, b) => b[1] - a[1]);
    const topRoles = Object.entries(roleCounts).sort((a, b) => b[1] - a[1]).slice(0, 8);
    return {
      total: history.length,
      topSkill: topSkills.length ? topSkills[0][0] : '—',
      avgScore: scoredCount ? `${(scoreSum / scoredCount).toFixed(1)}%` : null,
      roleLabels: topRoles.map(([k]) => k),
      roleValues: topRoles.map(([, v]) => v),
      skillLabels: topSkills.slice(0, 8).map(([k]) => k),
      skillValues: topSkills.slice(0, 8).map(([, v]) => v),
      monthLabels: Object.keys(byMonth).sort(),
      monthValues: Object.keys(byMonth).sort().map((k) => byMonth[k]),
      newModelCount: history.filter((h) => h.modelVersion).length,
    };
  }, [history]);

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    color: '#a1a1aa',
    scales: {
      y: { ticks: { color: '#a1a1aa', precision: 0 }, grid: { color: '#2f2f44' } },
      x: { ticks: { color: '#a1a1aa' }, grid: { display: false } },
    },
    plugins: { legend: { labels: { color: '#e4e4e7' } } },
  };

  const handleLogout = () => {
    localStorage.removeItem('jwt_token');
    navigate('/login');
  };

  return (
    <div>
      <div className="stars"></div>
      <div className="sidebar">
        <h1>EDU2JOB 2.0</h1>
        <p className="sidebar-tagline">Discover Your Career Fit</p>
        <button onClick={() => navigate('/home')}>
          <i className="fa-solid fa-house"></i> Home
        </button>
        <button className="active">
          <i className="fa-solid fa-box-archive"></i> View Archive
        </button>
        <button onClick={handleLogout} style={{ marginTop: 'auto', marginBottom: '30px' }}>
          <i className="fa-solid fa-right-from-bracket"></i> Logout
        </button>
      </div>

      <main className="main-content dashboard-grid">
        <div className="kpi-container">
          <div className="kpi-card">
            <i className="fa-solid fa-database kpi-icon"></i>
            <div className="kpi-details"><h3>Total Predictions</h3><p>{stats.total}</p></div>
          </div>
          <div className="kpi-card">
            <i className="fa-solid fa-bolt kpi-icon"></i>
            <div className="kpi-details">
              <h3>Most Common Skill</h3>
              <p style={{ fontSize: '1.2rem', marginTop: '10px' }}>{stats.topSkill}</p>
            </div>
          </div>
          <div className="kpi-card">
            <i className="fa-solid fa-robot kpi-icon"></i>
            <div className="kpi-details">
              <h3>New-model predictions</h3>
              <p>{stats.newModelCount}</p>
            </div>
          </div>
          {stats.avgScore && (
            <div className="kpi-card">
              <i className="fa-solid fa-crosshairs kpi-icon"></i>
              <div className="kpi-details">
                <h3>Avg Top-Role Score</h3><p>{stats.avgScore}</p>
              </div>
            </div>
          )}
        </div>

        {loading && <p className="empty-state">Loading your history…</p>}
        {error && <p className="prediction-error" role="alert">{error}</p>}
        {!loading && !error && history.length === 0 && (
          <p className="empty-state">
            No predictions yet. Head back home, complete your profile, and run
            your first prediction — it will appear here.
          </p>
        )}

        {!loading && history.length > 0 && (
          <>
            <div className="chart-grid">
              <div className="bento-box" style={{ height: '300px' }}>
                <h3>Most Frequently Recommended Roles</h3>
                <div style={{ height: '220px' }}>
                  <Bar data={{
                    labels: stats.roleLabels,
                    datasets: [{
                      label: 'Times recommended', data: stats.roleValues,
                      backgroundColor: 'rgba(0, 242, 254, 0.6)',
                      borderColor: '#00f2fe', borderWidth: 1, borderRadius: 4,
                    }],
                  }} options={chartOptions} />
                </div>
              </div>
              <div className="bento-box" style={{ height: '300px' }}>
                <h3>Your Skill Distribution</h3>
                <div style={{ height: '220px' }}>
                  <Doughnut data={{
                    labels: stats.skillLabels,
                    datasets: [{
                      data: stats.skillValues,
                      backgroundColor: ['#00f2fe', '#4facfe', '#818cf8', '#c084fc', '#e879f9', '#2dd4bf'],
                      borderWidth: 0,
                    }],
                  }} options={{ ...chartOptions, scales: {} }} />
                </div>
              </div>
              <div className="bento-box" style={{ height: '300px' }}>
                <h3>Prediction Activity</h3>
                <div style={{ height: '220px' }}>
                  <Line data={{
                    labels: stats.monthLabels,
                    datasets: [{
                      label: 'Predictions per month', data: stats.monthValues,
                      borderColor: '#00f2fe', backgroundColor: 'rgba(0,242,254,0.2)',
                      tension: 0.3, fill: true,
                    }],
                  }} options={chartOptions} />
                </div>
              </div>
            </div>

            <div className="bento-box" style={{ flex: 1, minHeight: '300px', display: 'flex', flexDirection: 'column' }}>
              <h3>Prediction History</h3>
              <div style={{ overflowY: 'auto', flex: 1, paddingRight: '5px' }}>
                <table id="predictionResultTable" style={{ width: '100%' }}>
                  <thead style={{ position: 'sticky', top: 0, background: '#2a2a40', zIndex: 1 }}>
                    <tr>
                      <th>Date</th><th>Degree / Major</th><th>CGPA</th>
                      <th>Top Recommendation</th><th>Model</th><th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((item) => {
                      const preds = Array.isArray(item.predictions) ? item.predictions : [];
                      const top = preds.length ? preds[0] : null;
                      const expanded = expandedId === item.id;
                      return (
                        <React.Fragment key={item.id}>
                          <tr>
                            <td style={{ color: '#a1a1aa' }}>
                              {item.created_at ? new Date(item.created_at).toLocaleDateString() : '—'}
                            </td>
                            <td style={{ color: '#fff' }}>
                              {[item.degree, item.major].filter(Boolean).join(' · ') || '—'}
                            </td>
                            <td style={{ color: '#a1a1aa' }}>{item.cgpa ?? '—'}</td>
                            <td style={{ color: '#4facfe', fontWeight: '600' }}>
                              {top ? top.role : '—'}
                            </td>
                            <td>
                              {item.modelVersion
                                ? <span className="model-badge">{item.modelVersion}</span>
                                : <span className="legacy-badge">{LEGACY_LABEL}</span>}
                            </td>
                            <td>
                              <button type="button" className="details-btn"
                                onClick={() => setExpandedId(expanded ? null : item.id)}>
                                {expanded ? 'Hide' : 'Details'}
                              </button>
                            </td>
                          </tr>
                          {expanded && (
                            <tr>
                              <td colSpan="6" style={{ color: '#a1a1aa' }}>
                                <strong style={{ color: '#fff' }}>Profile used:</strong>{' '}
                                Skills: {(Array.isArray(item.skills) ? item.skills : []).join(', ') || '—'}
                                {' · '}Certification: {item.certification_name || 'None'}
                                {' · '}Internship: {item.internship_experience || '—'}
                                {' · '}Projects: {item.projects_count ?? '—'}
                                {' · '}Communication: {item.communication_skill || '—'}
                                {' · '}Experience: {item.experience_months ?? '—'} months
                                <br />
                                <strong style={{ color: '#fff' }}>Recommended roles:</strong>{' '}
                                {preds.length
                                  ? preds.map((p) => `${p.rank || ''}. ${p.role}`.trim()).join('  ·  ')
                                  : 'No roles recorded'}
                              </td>
                            </tr>
                          )}
                        </React.Fragment>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
}

export default Archive;
