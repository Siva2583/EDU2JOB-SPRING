/**
 * Client-side validation + payload building for the nine-field schema.
 * Mirrors the Spring Boot Bean Validation rules so users get instant
 * feedback; the backend remains the authoritative validator.
 */

export function validateProfile(profile, { requireAll = false } = {}) {
  const errors = {};
  const p = profile || {};

  if (requireAll || p.degree) {
    if (!p.degree || !String(p.degree).trim()) errors.degree = 'Degree is required.';
  }
  if (requireAll || p.major) {
    if (!p.major || !String(p.major).trim()) errors.major = 'Major is required.';
  }
  if (requireAll || p.cgpa !== '' && p.cgpa !== null && p.cgpa !== undefined) {
    if (p.cgpa === '' || p.cgpa === null || p.cgpa === undefined) {
      errors.cgpa = 'CGPA is required.';
    } else {
      const cgpa = Number(p.cgpa);
      if (Number.isNaN(cgpa) || cgpa < 0 || cgpa > 10) {
        errors.cgpa = 'CGPA must be a number between 0 and 10 (10-point scale, not a percentage).';
      }
    }
  }
  if (requireAll || (Array.isArray(p.skills) && p.skills.length > 0)) {
    if (!Array.isArray(p.skills) || p.skills.length === 0) {
      errors.skills = 'Add at least one skill.';
    }
  } else if (p.skills !== undefined && !Array.isArray(p.skills)) {
    errors.skills = 'Skills must be a list.';
  }
  if (requireAll) {
    if (!p.internship_experience) errors.internship_experience = 'Please tell us about internship experience.';
    if (!p.communication_skill) errors.communication_skill = 'Please select your communication level.';
  }
  for (const field of ['projects_count', 'experience_months']) {
    const value = p[field];
    if (requireAll && (value === '' || value === null || value === undefined)) {
      errors[field] = field === 'projects_count' ? 'Projects count is required.' : 'Experience (months) is required.';
    } else if (value !== '' && value !== null && value !== undefined) {
      const num = Number(value);
      if (!Number.isInteger(num) || num < 0) {
        errors[field] = 'Must be a non-negative whole number.';
      }
    }
  }
  return errors;
}

/** Build the exact nine-field JSON body sent to Spring Boot / Flask. */
export function buildPredictionPayload(profile) {
  return {
    degree: String(profile.degree || '').trim(),
    major: String(profile.major || '').trim(),
    cgpa: Number(profile.cgpa),
    skills: (profile.skills || []).map((s) => String(s).trim()).filter(Boolean),
    certification_name: profile.certification_name
      ? String(profile.certification_name).trim()
      : null,
    internship_experience: profile.internship_experience,
    projects_count: Number(profile.projects_count),
    communication_skill: profile.communication_skill,
    experience_months: Number(profile.experience_months),
  };
}

/** Build a (possibly partial) profile-update body for POST /api/profile/update. */
export function buildProfilePayload(profile) {
  const payload = {};
  if (profile.degree !== undefined) payload.degree = String(profile.degree || '').trim();
  if (profile.major !== undefined) payload.major = String(profile.major || '').trim();
  if (profile.cgpa !== '' && profile.cgpa !== null && profile.cgpa !== undefined) {
    payload.cgpa = Number(profile.cgpa);
  }
  if (Array.isArray(profile.skills)) {
    payload.skills = profile.skills.map((s) => String(s).trim()).filter(Boolean);
  }
  payload.certification_name = profile.certification_name
    ? String(profile.certification_name).trim()
    : null;
  if (profile.internship_experience) payload.internship_experience = profile.internship_experience;
  if (profile.projects_count !== '' && profile.projects_count !== null && profile.projects_count !== undefined) {
    payload.projects_count = Number(profile.projects_count);
  }
  if (profile.communication_skill) payload.communication_skill = profile.communication_skill;
  if (profile.experience_months !== '' && profile.experience_months !== null && profile.experience_months !== undefined) {
    payload.experience_months = Number(profile.experience_months);
  }
  return payload;
}
