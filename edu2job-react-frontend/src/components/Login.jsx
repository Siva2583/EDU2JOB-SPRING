import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import '../App.css';

function Login() {
  const [isFlipped, setIsFlipped] = useState(false);
  const [activeForm, setActiveForm] = useState('');
  const [loginData, setLoginData] = useState({ username: '', password: '' });
  const [registerData, setRegisterData] = useState({ email: '', username: '', password1: '', password2: '' });
  const navigate = useNavigate();

  useEffect(() => {
    document.body.classList.add('login-body');
    return () => {
      document.body.classList.remove('login-body');
    };
  }, []);

  const handleLoginChange = (e) => setLoginData({ ...loginData, [e.target.name]: e.target.value });
  const handleRegisterChange = (e) => setRegisterData({ ...registerData, [e.target.name]: e.target.value });

  const handleFlip = (formType) => {
    setActiveForm(formType);
    setIsFlipped(true);
  };

  const handleBack = (e) => {
    e.preventDefault();
    setIsFlipped(false);
    setTimeout(() => setActiveForm(''), 400);
  };

const handleLoginSubmit = async (e) => {
    e.preventDefault();
    try {
      // Added /api/ to the path
      const response = await api.post('/api/auth/login', loginData);
      // Changed 'token' to 'jwt_token' to match your App.js router
      localStorage.setItem('jwt_token', response.data.token);
      navigate('/home');
    } catch (err) {
      alert("Login failed: " + (err.response?.data?.message || "Check your credentials."));
    }
  };

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    if (registerData.password1 !== registerData.password2) {
      alert("Passwords do not match!");
      return;
    }
    try {
      // Added /api/ to the path
      const response = await api.post('/api/auth/register', {
        email: registerData.email,
        username: registerData.username,
        password: registerData.password1
      });
      // Changed 'token' to 'jwt_token' to match your App.js router
      localStorage.setItem('jwt_token', response.data.token);
      alert(response.data.message); // Lets you know it succeeded!
      navigate('/home');
    } catch (err) {
      alert("Registration failed: " + (err.response?.data?.message || "Server error"));
    }
  };

  return (
    <div>
      <div className="top-panel">
        <div className="left">EDU2JOB</div>
      </div>

      <div id="input-box">
        <div className={`flip-card`}>
          <div className={`flip-inner ${isFlipped ? 'flipped' : ''}`}>

            <div className="face front">
              <h2>Welcome to EDU2JOB</h2>
              <p>Click below to proceed</p>
              <div className="bottom-buttons">
                <button className="btn" onClick={() => handleFlip('login')}>Login</button>
                <button className="btn" onClick={() => handleFlip('register')}>New User</button>
              </div>
            </div>

            <div className="face back">
              {activeForm === 'login' && (
                <form onSubmit={handleLoginSubmit} style={{ width: '100%' }}>
                  <h2>Login</h2>
                  <div className="input-row">
                    <label>Username</label>
                    <input name="username" type="text" onChange={handleLoginChange} required />
                  </div>
                  <div className="input-row">
                    <label>Password</label>
                    <input name="password" type="password" onChange={handleLoginChange} required />
                  </div>
                  <button type="submit" className="btn" style={{ marginTop: '15px' }}>Login</button>
                  <div style={{ textAlign: 'center' }}>
                    <span onClick={handleBack} className="muted-link">Back</span>
                  </div>
                </form>
              )}

              {activeForm === 'register' && (
                <form onSubmit={handleRegisterSubmit} style={{ width: '100%' }}>
                  <h2>Register</h2>
                  <div className="input-row">
                    <label>Email</label>
                    <input name="email" type="email" onChange={handleRegisterChange} required />
                  </div>
                  <div className="input-row">
                    <label>Username</label>
                    <input name="username" type="text" onChange={handleRegisterChange} required />
                  </div>
                  <div className="input-row">
                    <label>Password</label>
                    <input name="password1" type="password" onChange={handleRegisterChange} required />
                  </div>
                  <div className="input-row">
                    <label>Confirm Password</label>
                    <input name="password2" type="password" onChange={handleRegisterChange} required />
                  </div>
                  <button type="submit" className="btn" style={{ marginTop: '15px' }}>Register</button>
                  <div style={{ textAlign: 'center' }}>
                    <span onClick={handleBack} className="muted-link">Back</span>
                  </div>
                </form>
              )}
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;