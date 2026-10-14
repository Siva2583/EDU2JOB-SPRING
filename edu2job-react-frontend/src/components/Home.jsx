import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { apiErrorMessage } from '../api';
import '../App.css';
import ProfileForm from './ProfileForm';
import ResultsCards from './ResultsCards';
import { EMPTY_PROFILE, profileCompletion } from '../constants/profileOptions';
import { buildPredictionPayload, buildProfilePayload, validateProfile } from '../utils/profileValidation';

function Home() {
  const navigate = useNavigate();
  const [activeView, setActiveView] = useState('dashboard');
  const [toastMessage, setToastMessage] = useState('');
  const [profile, setProfile] = useState(EMPTY_PROFILE);
  const [profileErrors, setProfileErrors] = useState({});
  const [profileMessage, setProfileMessage] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const [predictions, setPredictions] = useState([]);
  const [modelVersion, setModelVersion] = useState(null);
  const [predictionError, setPredictionError] = useState('');
  const [isPredicting, setIsPredicting] = useState(false);

  const [history, setHistory] = useState([]);
  const [historyError, setHistoryError] = useState('');

  const showToast = (message) => {
    setToastMessage(message);
    setTimeout(() => setToastMessage(''), 3000);
  };

  const applyServerProfile = useCallback((data) => {
    setProfile({
      degree: data.degree || '',
      major: data.major || '',
      cgpa: data.cgpa ?? '',
      skills: Array.isArray(data.skills) ? data.skills : [],
      certification_name: data.certification_name || '',
      internship_experience: data.internship_experience || '',
      projects_count: data.projects_count ?? '',
      communication_skill: data.communication_skill || '',
      experience_months: data.experience_months ?? '',
    });
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const res = await api.get('/api/profile/latest');
        if (res.data && !res.data.message) applyServerProfile(res.data);
      } catch (err) { /* profile may simply not exist yet */ }
      try {
        const res = await api.get('/api/predict/history');
        setHistory(Array.isArray(res.data) ? res.data : []);
      } catch (err) {
        setHistoryError(apiErrorMessage(err, 'Could not load your history.'));
      }
    })();
  }, [applyServerProfile]);

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    const errors = validateProfile(profile);
    setProfileErrors(errors);
    if (Object.keys(errors).length > 0) return;
    setIsSaving(true);
    setProfileMessage('');
    try {
      const res = await api.post('/api/profile/update', buildProfilePayload(profile));
      if (res.data && !res.data.message) applyServerProfile(res.data);
      showToast('Profile updated successfully!');
    } catch (err) {
      setProfileMessage(apiErrorMessage(err, 'Could not save your profile.'));
    } finally {
      setIsSaving(false);
    }
  };

  const handlePredictSubmit = async (e) => {
    e.preventDefault();
    const errors = validateProfile(profile, { requireAll: true });
    setProfileErrors(errors);
    if (Object.keys(errors).length > 0) {
      setPredictionError('Your profile is incomplete — please fill in every field so the model gets all nine inputs.');
      return;
    }
    setIsPredicting(true);
    setPredictions([]);
    setPredictionError('');
    try {
      const res = await api.post('/api/predict/', buildPredictionPayload(profile));
      if (res.data.predictions && res.data.predictions.length > 0) {
        setPredictions(res.data.predictions);
        setModelVersion(res.data.modelVersion || 'edu2job-v2');
        const hist = await api.get('/api/predict/history').catch(() => null);
        if (hist) setHistory(Array.isArray(hist.data) ? hist.data : []);
      } else {
        setPredictionError('No recommendations came back. Please try again.');
      }
    } catch (err) {
      setPredictionError(apiErrorMessage(err, 'Prediction failed. Please try again.'));
    } finally {
      setIsPredicting(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('jwt_token');
    navigate('/login');
  };

  const completed = profileCompletion(profile);
  const latest = history.length > 0 ? history[0] : null;
  const latestRoles = latest && Array.isArray(latest.predictions) ? latest.predictions : [];

  return (
    <>
      <div className="stars"></div>
      <div className="sidebar">
        <h1>EDU2JOB 2.0</h1>
        <p className="sidebar-tagline">Discover Your Career Fit</p>
        <button className={activeView === 'dashboard' ? 'active' : ''} onClick={() => setActiveView('dashboard')}>
          <i className="fa-solid fa-house"></i> Dashboard
        </button>
        <button className={activeView === 'profile' ? 'active' : ''} onClick={() => setActiveView('profile')}>
          <i className="fa-solid fa-user-gear"></i> Profile Info
        </button>
        <button className={activeView === 'predict' ? 'active' : ''} onClick={() => setActiveView('predict')}>
          <i className="fa-solid fa-brain"></i> Predict
        </button>
        <button className="archive" onClick={() => navigate('/archive')}>
          <i className="fa-solid fa-box-archive"></i> View Archive
        </button>
        <button onClick={handleLogout} style={{ marginTop: 'auto', marginBottom: '30px' }}>
          <i className="fa-solid fa-right-from-bracket"></i> Logout
        </button>
      </div>

      <main className="main-content">
        {activeView === 'dashboard' && (
          <div id="welcome-screen">
            <h1 className="welcome-title">Discover Your Career Fit 🎓</h1>
            <p className="welcome-subtitle">
              EDU2JOB 2.0 matches your degree, skills and experience to the
              job roles that fit you best — technical or non-technical.
            </p>

            <div className="kpi-container">
              <div className="kpi-card">
                <i className="fa-solid fa-user-check kpi-icon"></i>
                <div className="kpi-details">
                  <h3>Profile completion</h3>
                  <p>{completed}/9 fields</p>
                </div>
              </div>
              <div className="kpi-card">
                <i className="fa-solid fa-database kpi-icon"></i>
                <div className="kpi-details">
                  <h3>Predictions made</h3>
                  <p>{history.length}</p>
                </div>
              </div>
              <div className="kpi-card">
                <i className="fa-solid fa-crosshairs kpi-icon"></i>
                <div className="kpi-details">
                  <h3>Latest top role</h3>
                  <p style={{ fontSize: '1.1rem', marginTop: '10px' }}>
                    {latestRoles.length > 0 ? latestRoles[0].role : '—'}
                  </p>
                </div>
              </div>
            </div>

            <div className="quick-actions-container">
              <div className="quick-action-card" onClick={() => setActiveView('profile')}>
                <i className="fa-solid fa-address-card"></i>
                <h3>{completed === 9 ? 'Update Profile' : 'Complete Your Profile'}</h3>
                <p>{completed === 9
                  ? 'Keep your details current for the best predictions.'
                  : `You have filled ${completed} of 9 fields. Complete your profile to unlock predictions.`}</p>
              </div>
              <div className="quick-action-card" onClick={() => setActiveView('predict')}>
                <i className="fa-solid fa-wand-magic-sparkles"></i>
                <h3>Get Career Recommendations</h3>
                <p>See your top 5 job roles, ranked for your profile.</p>
              </div>
            </div>

            {historyError && <p className="field-error">{historyError}</p>}
            {latestRoles.length > 0 && (
              <section className="bento-box" style={{ marginTop: '24px' }}>
                <h3>Your most recent recommendations</h3>
                <ResultsCards predictions={latestRoles.slice(0, 5)}
                  modelVersion={latest.modelVersion || undefined} />
                {!latest.modelVersion && (
                  <p className="results-note">These came from the previous model version and are kept for your records.</p>
                )}
              </section>
            )}
            {history.length === 0 && !historyError && (
              <p className="empty-state">
                No predictions yet. Complete your profile, then run your first
                career prediction to see recommendations here.
              </p>
            )}
          </div>
        )}

        {activeView === 'profile' && (
          <form id="profile-form" onSubmit={handleProfileSubmit}>
            <h2 className="view-title">Your Profile</h2>
            <p className="view-subtitle">
              This is exactly what the recommendation model sees — all nine
              fields, nothing more, nothing hidden.
            </p>
            <ProfileForm profile={profile} onChange={setProfile}
              errors={profileErrors} disabled={isSaving} />
            {profileMessage && <p className="field-error">{profileMessage}</p>}
            <button type="submit" id="submitProfile" disabled={isSaving}>
              {isSaving ? 'Saving…' : 'Save Profile'}
            </button>
          </form>
        )}

        {activeView === 'predict' && (
          <div>
            <h2 className="view-title">Career Prediction</h2>
            <p className="view-subtitle">
              Predictions run on your profile below. Edit anything, then run —
              the same nine fields are sent to the model.
            </p>
            <form id="predict-form" onSubmit={handlePredictSubmit}>
              <ProfileForm profile={profile} onChange={setProfile}
                errors={profileErrors} disabled={isPredicting} />
              <button type="submit" id="predictSubmit" disabled={isPredicting}>
                {isPredicting ? '🧠 Analyzing your profile…' : 'Get My Top 5 Roles'}
              </button>
            </form>

            {predictionError && !isPredicting && (
              <p className="prediction-error" role="alert">{predictionError}</p>
            )}
            {predictions.length > 0 && (
              <section id="predictionResultContainer">
                <h2>✨ Your Predicted Career Paths</h2>
                <ResultsCards predictions={predictions} modelVersion={modelVersion} />
              </section>
            )}
          </div>
        )}

        <div id="toast-container">
          <div className={`toast ${toastMessage ? 'show' : ''}`}>
            <i className="fa-solid fa-circle-check"></i> {toastMessage}
          </div>
        </div>
      </main>
    </>
  );
}

export default Home;
