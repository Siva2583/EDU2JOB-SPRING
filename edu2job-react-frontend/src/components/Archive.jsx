import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, ArcElement } from 'chart.js';
import { Bar, Doughnut } from 'react-chartjs-2';
import api from '../api';
import '../App.css';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, ArcElement);

function Archive() {
  const [history, setHistory] = useState([]);
  const [chartData, setChartData] = useState({ roles: {}, skills: {}, cgpa: {} });
  const [kpis, setKpis] = useState({ total: 0, topSkill: '-', avgScore: 0 });
  const navigate = useNavigate();

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    try {
      const res = await api.get('/api/predict/history');
      const data = Array.isArray(res.data) ? res.data : [];
      setHistory(data);
      processChartData(data);
    } catch (err) {
      console.error(err);
    }
  };

  const processChartData = (data) => {
    const rolesCount = {};
    const skillsCount = {};
    const cgpaByMajor = {};
    let totalConfidence = 0;
    let confidenceCount = 0;

    data.forEach(item => {
      try {
        const parsedData = JSON.parse(item.predictedOutput);
        const preds = parsedData.predictions || [];
        if (preds && preds.length > 0) {
          const topRole = preds[0].role;
          rolesCount[topRole] = (rolesCount[topRole] || 0) + 1;
          
          // Calculate avg confidence score
          const scoreStr = preds[0].score.replace('%', '');
          const scoreVal = parseFloat(scoreStr);
          if (!isNaN(scoreVal)) {
            totalConfidence += scoreVal;
            confidenceCount++;
          }
        }
      } catch (e) {}

      if (item.skills) {
        item.skills.split(',').forEach(skill => {
          const s = skill.trim();
          if (s) skillsCount[s] = (skillsCount[s] || 0) + 1;
        });
      }

      const major = item.major;
      const cgpa = parseFloat(item.cgpa);
      if (major && !isNaN(cgpa)) {
        if (!cgpaByMajor[major]) cgpaByMajor[major] = { sum: 0, count: 0 };
        cgpaByMajor[major].sum += cgpa;
        cgpaByMajor[major].count += 1;
      }
    });

    const sortedSkills = Object.entries(skillsCount).sort((a, b) => b[1] - a[1]);
    const top10Skills = sortedSkills.slice(0, 10);
    const skillsObj = {};
    top10Skills.forEach(([k, v]) => skillsObj[k] = v);

    const avgCgpa = {};
    Object.keys(cgpaByMajor).forEach(major => {
      avgCgpa[major] = cgpaByMajor[major].sum / cgpaByMajor[major].count;
    });

    // Set KPIs
    setKpis({
      total: data.length,
      topSkill: sortedSkills.length > 0 ? sortedSkills[0][0] : 'N/A',
      avgScore: confidenceCount > 0 ? (totalConfidence / confidenceCount).toFixed(1) + '%' : '0%'
    });

    setChartData({ roles: rolesCount, skills: skillsObj, cgpa: avgCgpa });
  };

  const roleChartConfig = {
    labels: Object.keys(chartData.roles),
    datasets: [{
      label: 'Frequency',
      data: Object.values(chartData.roles),
      backgroundColor: 'rgba(0, 242, 254, 0.6)',
      borderColor: '#00f2fe',
      borderWidth: 1,
      borderRadius: 4
    }]
  };

  const skillsChartConfig = {
    labels: Object.keys(chartData.skills),
    datasets: [{
      data: Object.values(chartData.skills),
      backgroundColor: ['#00f2fe', '#4facfe', '#818cf8', '#c084fc', '#e879f9', '#2dd4bf'],
      borderWidth: 0
    }]
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    color: '#a1a1aa',
    scales: {
      y: { ticks: { color: '#a1a1aa' }, grid: { color: '#2f2f44' } },
      x: { ticks: { color: '#a1a1aa' }, grid: { display: false } }
    },
    plugins: {
      legend: { labels: { color: '#e4e4e7' } }
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('jwt_token');
    navigate('/login');
  };

  return (
    <div>
      <div className="stars"></div>
      <div className="sidebar">
        <h1>Welcome Back!</h1>
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
        
        {/* TOP ROW: KPI CARDS */}
        <div className="kpi-container">
          <div className="kpi-card">
            <i className="fa-solid fa-database kpi-icon"></i>
            <div className="kpi-details">
              <h3>Total Predictions</h3>
              <p>{kpis.total}</p>
            </div>
          </div>
          <div className="kpi-card">
            <i className="fa-solid fa-bolt kpi-icon"></i>
            <div className="kpi-details">
              <h3>Most Common Skill</h3>
              <p style={{fontSize: '1.2rem', marginTop: '10px'}}>{kpis.topSkill}</p>
            </div>
          </div>
          <div className="kpi-card">
            <i className="fa-solid fa-crosshairs kpi-icon"></i>
            <div className="kpi-details">
              <h3>Avg AI Confidence</h3>
              <p>{kpis.avgScore}</p>
            </div>
          </div>
        </div>

        {/* MIDDLE ROW: CHARTS */}
        <div className="chart-grid">
          <div className="bento-box" style={{ height: '300px' }}>
             <h3>Top Predicted Roles</h3>
             <div style={{ height: '220px' }}>
                <Bar data={roleChartConfig} options={chartOptions} />
             </div>
          </div>
          <div className="bento-box" style={{ height: '300px' }}>
             <h3>Skill Distribution</h3>
             <div style={{ height: '220px' }}>
                <Doughnut data={skillsChartConfig} options={{ ...chartOptions, scales: {} }} />
             </div>
          </div>
        </div>

        {/* BOTTOM ROW: TABLE */}
        <div className="bento-box" style={{ flex: 1, minHeight: '300px', display: 'flex', flexDirection: 'column' }}>
          <h3>Raw History Log</h3>
          <div style={{ overflowY: 'auto', flex: 1, paddingRight: '5px' }}>
            <table id="predictionResultTable" style={{ width: '100%' }}>
              <thead style={{ position: 'sticky', top: 0, background: '#2a2a40', zIndex: 1 }}>
                <tr>
                  <th>Major</th>
                  <th>CGPA</th>
                  <th>Degree</th>
                  <th>Skills</th>
                  <th>Top Prediction</th>
                </tr>
              </thead>
              <tbody>
                {history.length === 0 ? (
                  <tr><td colSpan="5" style={{ textAlign: 'center', color: '#a1a1aa', padding: '20px' }}>No history yet.</td></tr>
                ) : (
                  history.map((pred, idx) => {
                    let topRole = "N/A", topScore = "N/A";
                    try {
                      const preds = JSON.parse(pred.predictedOutput).predictions || [];
                      if (preds.length > 0) {
                        topRole = preds[0].role;
                        topScore = preds[0].score;
                      }
                    } catch (e) {}

                    return (
                      <tr key={idx}>
                        <td style={{ color: '#fff' }}>{pred.major}</td>
                        <td style={{ color: '#a1a1aa' }}>{pred.cgpa}</td>
                        <td style={{ color: '#a1a1aa' }}>{pred.degree}</td>
                        <td style={{ color: '#a1a1aa' }}>{pred.skills}</td>
                        <td style={{ color: '#4facfe', fontWeight: '600' }}>{topRole} ({topScore})</td>
                      </tr>
                    )
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

      </main>
    </div>
  );
}

export default Archive;