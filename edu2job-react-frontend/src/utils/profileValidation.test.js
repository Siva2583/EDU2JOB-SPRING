import {
  buildPredictionPayload, buildProfilePayload, validateProfile,
} from './profileValidation';

const COMPLETE = {
  degree: 'BBA',
  major: 'Finance',
  cgpa: '8.2',
  skills: ['Excel', 'Financial Analysis'],
  certification_name: '',
  internship_experience: 'Yes',
  projects_count: '2',
  communication_skill: 'Intermediate',
  experience_months: '4',
};

describe('validateProfile', () => {
  test('accepts a complete non-technical profile', () => {
    expect(validateProfile(COMPLETE, { requireAll: true })).toEqual({});
  });

  test('rejects CGPA outside the 0-10 scale', () => {
    const errors = validateProfile({ ...COMPLETE, cgpa: '85' }, { requireAll: true });
    expect(errors.cgpa).toMatch(/between 0 and 10/);
  });

  test('rejects negative project / experience counts', () => {
    const errors = validateProfile(
      { ...COMPLETE, projects_count: '-1', experience_months: '-3' },
      { requireAll: true });
    expect(errors.projects_count).toBeTruthy();
    expect(errors.experience_months).toBeTruthy();
  });

  test('requires at least one skill for prediction', () => {
    const errors = validateProfile({ ...COMPLETE, skills: [] }, { requireAll: true });
    expect(errors.skills).toBeTruthy();
  });
});

describe('buildPredictionPayload', () => {
  test('emits exactly the nine contract fields with JSON types', () => {
    const payload = buildPredictionPayload(COMPLETE);
    expect(Object.keys(payload)).toEqual([
      'degree', 'major', 'cgpa', 'skills', 'certification_name',
      'internship_experience', 'projects_count', 'communication_skill',
      'experience_months',
    ]);
    expect(payload.cgpa).toBe(8.2);
    expect(payload.skills).toEqual(['Excel', 'Financial Analysis']);
    // Empty certification means "none" -> null, never an invented value.
    expect(payload.certification_name).toBeNull();
    expect(payload.projects_count).toBe(2);
  });
});

describe('buildProfilePayload', () => {
  test('omits blank numeric fields (partial update)', () => {
    const payload = buildProfilePayload({ ...COMPLETE, cgpa: '', projects_count: '' });
    expect(payload.cgpa).toBeUndefined();
    expect(payload.projects_count).toBeUndefined();
    expect(payload.degree).toBe('BBA');
  });
});
