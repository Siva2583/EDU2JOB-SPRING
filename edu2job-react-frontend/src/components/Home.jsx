import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import '@yaireo/tagify/dist/tagify.css';
import Tagify from '@yaireo/tagify';
import '../App.css';

function Home() {
  const navigate = useNavigate();
  const [activeView, setActiveView] = useState('welcome');
  const [isEditing, setIsEditing] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [predictions, setPredictions] = useState([]);
  const [predictionError, setPredictionError] = useState('');
  const [isPredicting, setIsPredicting] = useState(false);
  const [username, setUsername] = useState('Explorer');

  const [profileData, setProfileData] = useState({
    username: '', email: '', cgpa: '', degree: '', skills: '', yop: ''
  });

  const [predictData, setPredictData] = useState({
    major: '', cgpa: '', degree: '', skills: '', yop: ''
  });

  const profileSkillsRef = useRef();
  const predictSkillsRef = useRef();
  const profileTagify = useRef();
  const predictTagify = useRef();

  const skillsWhitelist = ["Python","Communication","DataStructures","Algorithms","SQL","Java","C++","JavaScript","HTML","CSS","Git","Linux","Django","React","Matlab","Excel","Accounting","Financial Modeling","Electronics","Microcontrollers","RTOS","SolidWorks","AutoCAD","Lean Manufacturing","Thermodynamics","Mechanics","Materials","CAD","Valuation","M&A"];

  useEffect(() => {
    if (activeView === 'profile' && profileSkillsRef.current && !profileTagify.current) {
      profileTagify.current = new Tagify(profileSkillsRef.current, {
        whitelist: skillsWhitelist,
        dropdown: { enabled: 1, maxItems: 20, closeOnSelect: true }
      });
      profileTagify.current.on('change', (e) => {
        setProfileData(prev => ({ ...prev, skills: e.detail.value }));
      });
      
      if (profileData.skills) {
        try {
          const parsed = JSON.parse(profileData.skills);
          profileTagify.current.addTags(parsed);
        } catch {
          profileTagify.current.addTags(profileData.skills.split(','));
        }
      }
    }
    
    if (activeView === 'predict' && predictSkillsRef.current && !predictTagify.current) {
      predictTagify.current = new Tagify(predictSkillsRef.current, {
        whitelist: skillsWhitelist,
        dropdown: { enabled: 1, maxItems: 20, closeOnSelect: true }
      });
      predictTagify.current.on('change', (e) => {
        setPredictData(prev => ({ ...prev, skills: e.detail.value }));
      });
    }
  }, [activeView, profileData.skills]);

  
useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await api.get('/api/profile/latest');
        if (res.data && !res.data.message) {
          setUsername(res.data.username || 'Explorer');
          setProfileData({
            username: res.data.username || '',
            email: res.data.email || '',
            cgpa: res.data.cgpa || '',
            degree: res.data.degree || '',
            skills: res.data.skills ? res.data.skills.join(',') : '',
            yop: res.data.year_of_graduation || ''
          });
          setIsEditing(!res.data.cgpa);
        }
      } catch (err) {
        console.error(err);
      }
    };
    fetchProfile();
  }, []);

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    try {
      let parsedSkills = [];
      if (profileData.skills) {
        try { 
          parsedSkills = JSON.parse(profileData.skills).map(t => t.value); 
        } catch { 
          parsedSkills = profileData.skills.split(','); 
        }
      }
      
      const payload = { ...profileData, skills: parsedSkills.join(',') };
      await api.post('/api/profile/update', payload);
      setIsEditing(false);
      showToast('Profile updated successfully!');
    } catch (err) {
      console.error(err);
    }
  };

  const handlePredictSubmit = async (e) => {
    e.preventDefault();
    setIsPredicting(true);
    setPredictions([]);
    setPredictionError('');

    try {
      let parsedSkills = [];
      if (predictData.skills) {
        try { 
          parsedSkills = JSON.parse(predictData.skills).map(t => t.value); 
        } catch { 
          parsedSkills = predictData.skills.split(','); 
        }
      }

      const payload = { ...predictData, skills: parsedSkills.join(',') };
      const res = await api.post('/api/predict/', payload);
      
      if (res.data.predictions && res.data.predictions.length > 0) {
        setPredictions(res.data.predictions);
      } else {
        setPredictionError(res.data.error || "Could not retrieve prediction.");
      }
    } catch (err) {
      setPredictionError("An error occurred.");
    } finally {
      setIsPredicting(false);
    }
  };
  const showToast = (message) => {
    setToastMessage(message);
    setTimeout(() => setToastMessage(''), 3000);
  };





  return (
    <>
      <div id="stars1"></div>
      <div id="stars2"></div>
      <div id="stars3"></div>

      <div className="sidebar">
        <h1>Welcome Back!</h1>
        <button className={`profile-info ${activeView === 'profile' ? 'active' : ''}`} onClick={() => setActiveView('profile')}>
          <i className="fa-solid fa-user-gear"></i> Profile Info
        </button>
        <button className={`predict ${activeView === 'predict' ? 'active' : ''}`} onClick={() => setActiveView('predict')}>
          <i className="fa-solid fa-brain"></i> Predict
        </button>
        <button className="archive" onClick={() => navigate('/archive')}>
          <i className="fa-solid fa-box-archive"></i> View Archive
        </button>
      </div>

      <main className="main-content">
        {activeView === 'welcome' && (
          <div id="welcome-screen">
            <h1 className="welcome-title">Hello, {username}!</h1>
            <p className="welcome-subtitle">What would you like to do today?</p>
            <div className="quick-actions-container">
              <div className="quick-action-card" onClick={() => setActiveView('profile')}>
                <i className="fa-solid fa-address-card"></i>
                <h3>Update Profile</h3>
                <p>Keep your details current for the best predictions.</p>
              </div>
              <div className="quick-action-card" onClick={() => setActiveView('predict')}>
                <i className="fa-solid fa-wand-magic-sparkles"></i>
                <h3>Get Prediction</h3>
                <p>Discover your potential career paths right now.</p>
              </div>
            </div>
          </div>
        )}

        {activeView === 'profile' && (
          <form id="profile-form" style={{ display: 'flex' }} onSubmit={handleProfileSubmit}>
            <label htmlFor="username">Username</label>
            <input type="text" id="username" value={profileData.username} readOnly />
            
            <label htmlFor="email">Email</label>
            <input type="email" id="email" value={profileData.email} readOnly />
            
            <label htmlFor="profile-cgpa">CGPA</label>
            <input type="number" id="profile-cgpa" value={profileData.cgpa} disabled={!isEditing} onChange={(e) => setProfileData({...profileData, cgpa: e.target.value})} />
            
            <label htmlFor="profile-degree">Degree</label>
            <input type="text" id="profile-degree" value={profileData.degree} disabled={!isEditing} onChange={(e) => setProfileData({...profileData, degree: e.target.value})} />
            
            <label htmlFor="profile-skills">Skills</label>
            <input ref={profileSkillsRef} id="profile-skills" placeholder="Type or select skills..." disabled={!isEditing} />
            
            <label htmlFor="profile-yearofpass">Year of Graduation:</label>
            <input type="number" id="profile-yearofpass" value={profileData.yop} disabled={!isEditing} onChange={(e) => setProfileData({...profileData, yop: e.target.value})} />
            
            {isEditing ? (
              <button type="submit" id="submitProfile">Submit</button>
            ) : (
              <button type="button" id="editProfile" onClick={() => setIsEditing(true)} style={{ display: 'inline-block' }}>Edit</button>
            )}
          </form>
        )}

        {activeView === 'predict' && (
          <>
            <form id="predict-form" style={{ display: predictions.length || isPredicting || predictionError ? 'none' : 'flex' }} onSubmit={handlePredictSubmit}>
              <label htmlFor="major">Major:</label>
              <input id="major" list="majors" placeholder="Select your major" value={predictData.major} onChange={(e) => setPredictData({...predictData, major: e.target.value})} />
              <datalist id="majors">
                <option value="Civil" />
                <option value="Mechanical" />
                <option value="Business" />
                <option value="Computer Science" />
                <option value="Finance" />
                <option value="Electronics" />
              </datalist>

              <label htmlFor="predict-cgpa">CGPA</label>
              <input type="number" id="predict-cgpa" value={predictData.cgpa} onChange={(e) => setPredictData({...predictData, cgpa: e.target.value})} />

              <label htmlFor="predict-degree">Degree:</label>
              <input id="predict-degree" list="degrees" placeholder="Select your degree" value={predictData.degree} onChange={(e) => setPredictData({...predictData, degree: e.target.value})} />
              <datalist id="degrees">
                <option value="MBA" />
                <option value="M.Sc" />
                <option value="B.Tech" />
                <option value="M.Tech" />
                <option value="B.Sc" />
              </datalist>

              <label htmlFor="predict-skills">Skills</label>
              <input ref={predictSkillsRef} id="predict-skills" placeholder="Type or select skills..." />

              <label htmlFor="predict-yearofpass">Year of Graduation:</label>
              <input type="number" id="predict-yearofpass" value={predictData.yop} onChange={(e) => setPredictData({...predictData, yop: e.target.value})} />

              <button type="submit" id="predictSubmit">Predict</button>
            </form>

            {(isPredicting || predictions.length > 0 || predictionError) && (
              <div id="predictionResultContainer" style={{ display: 'block' }}>
                <h2>✨ Your Predicted Career Paths</h2>
                <table id="predictionResultTable">
                  <thead>
                    <tr>
                      <th style={{ width: '30%' }}>Role</th>
                      <th style={{ width: '25%' }}>Confidence</th>
                      <th>Justification & Scope</th>
                    </tr>
                  </thead>
                  <tbody>
                    {isPredicting && (
                      <tr><td colSpan="3" style={{ textAlign: 'center' }}>🧠 Analyzing your profile...</td></tr>
                    )}
                    {predictionError && !isPredicting && (
                      <tr><td colSpan="3" style={{ textAlign: 'center' }}>{predictionError}</td></tr>
                    )}
                    {!isPredicting && predictions.map((pred, idx) => (
                      <tr key={idx} className="fade-in-row">
                        <td><strong>{pred.role}</strong></td>
                        <td>
                          <div className="confidence-bar">
                            <div className="confidence-bar-inner" style={{ width: `${pred.score}%`, backgroundColor: parseFloat(pred.score) > 75 ? '#28a745' : parseFloat(pred.score) > 50 ? '#ffc107' : '#dc3545' }}>
                              {pred.score}%
                            </div>
                          </div>
                        </td>
                        <td>{pred.scope}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {!isPredicting && (
                   <button onClick={() => { setPredictions([]); setPredictionError(''); }} style={{ marginTop: '20px' }}>New Prediction</button>
                )}
              </div>
            )}
          </>
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