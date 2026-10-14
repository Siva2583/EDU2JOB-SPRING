import React from 'react';
import SkillsInput from './SkillsInput';
import {
  CERTIFICATION_OPTIONS, COMMUNICATION_OPTIONS, DEGREE_OPTIONS,
  INTERNSHIP_OPTIONS, MAJOR_OPTIONS,
} from '../constants/profileOptions';

/**
 * The nine-field profile form, grouped into three sections:
 *   1. Academic background  — degree, major, cgpa
 *   2. Skills & certifications — skills, certification_name
 *   3. Experience & readiness — internship_experience, projects_count,
 *      communication_skill, experience_months
 *
 * Used both for saving the profile and as the prediction input (predictions
 * run on the profile, so what you save is exactly what the model sees).
 */
function ProfileForm({ profile, onChange, errors = {}, disabled = false }) {
  const set = (field) => (e) => onChange({ ...profile, [field]: e.target.value });
  const fieldError = (field) => errors[field]
    ? <span className="field-error" role="alert">{errors[field]}</span> : null;

  return (
    <div className="profile-form">
      <fieldset className="form-section" disabled={disabled}>
        <legend>🎓 Academic background</legend>

        <label htmlFor="pf-degree">Degree</label>
        <select id="pf-degree" value={profile.degree} onChange={set('degree')}>
          <option value="">Select your degree…</option>
          {DEGREE_OPTIONS.map((d) => <option key={d} value={d}>{d}</option>)}
        </select>
        {fieldError('degree')}

        <label htmlFor="pf-major">Major / specialization</label>
        <select id="pf-major" value={profile.major} onChange={set('major')}>
          <option value="">Select your major…</option>
          {MAJOR_OPTIONS.map((m) => <option key={m} value={m}>{m}</option>)}
        </select>
        <span className="field-hint">Technical and non-technical backgrounds are equally supported.</span>
        {fieldError('major')}

        <label htmlFor="pf-cgpa">CGPA (10-point scale)</label>
        <input id="pf-cgpa" type="number" min="0" max="10" step="0.01"
          placeholder="e.g. 8.5"
          value={profile.cgpa} onChange={set('cgpa')} />
        <span className="field-hint">Enter your CGPA out of 10 — not a percentage.</span>
        {fieldError('cgpa')}
      </fieldset>

      <fieldset className="form-section" disabled={disabled}>
        <legend>🛠️ Skills &amp; certifications</legend>

        <label htmlFor="skills-input">Skills</label>
        <SkillsInput id="skills-input" value={profile.skills}
          onChange={(skills) => onChange({ ...profile, skills })} disabled={disabled} />
        <span className="field-hint">
          Add everything relevant — coding, tools, or people skills like
          Communication and Leadership. Press Enter to add each one.
        </span>
        {fieldError('skills')}

        <label htmlFor="pf-certification">Certification (optional)</label>
        <input id="pf-certification" type="text" list="certification-options"
          placeholder="e.g. AWS Certified Cloud Practitioner — leave empty if none"
          value={profile.certification_name} onChange={set('certification_name')} />
        <datalist id="certification-options">
          {CERTIFICATION_OPTIONS.map((c) => <option key={c} value={c} />)}
        </datalist>
        <span className="field-hint">Leave empty if you have no certification — that is a valid answer.</span>
        {fieldError('certification_name')}
      </fieldset>

      <fieldset className="form-section" disabled={disabled}>
        <legend>🚀 Experience &amp; readiness</legend>

        <label htmlFor="pf-internship">Internship experience</label>
        <select id="pf-internship" value={profile.internship_experience}
          onChange={set('internship_experience')}>
          <option value="">Select…</option>
          {INTERNSHIP_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
        {fieldError('internship_experience')}

        <label htmlFor="pf-projects">Projects completed</label>
        <input id="pf-projects" type="number" min="0" step="1" placeholder="e.g. 3"
          value={profile.projects_count} onChange={set('projects_count')} />
        <span className="field-hint">Academic, personal, or freelance projects all count.</span>
        {fieldError('projects_count')}

        <label htmlFor="pf-communication">Communication skill</label>
        <select id="pf-communication" value={profile.communication_skill}
          onChange={set('communication_skill')}>
          <option value="">Select your level…</option>
          {COMMUNICATION_OPTIONS.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        {fieldError('communication_skill')}

        <label htmlFor="pf-experience">Relevant experience (months)</label>
        <input id="pf-experience" type="number" min="0" step="1" placeholder="e.g. 6"
          value={profile.experience_months} onChange={set('experience_months')} />
        <span className="field-hint">
          Months of relevant experience (work, internships, freelancing).
          Enter 0 if you are just starting out.
        </span>
        {fieldError('experience_months')}
      </fieldset>
    </div>
  );
}

export default ProfileForm;
