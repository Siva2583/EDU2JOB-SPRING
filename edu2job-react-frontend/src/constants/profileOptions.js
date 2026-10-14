/**
 * Option lists for the nine-field EDU2JOB 2.0 profile form.
 *
 * PROVISIONAL: the definitive category vocabularies come from the new
 * model's training data (edu2job_dataset.csv) and are delivered with the
 * model artifacts via ml_models/v2/model_metadata.json. Once those are
 * confirmed, trim/extend these lists to match — the Flask service enforces
 * the metadata vocabularies authoritatively and returns a clear 400 for
 * anything unsupported, so the form can never silently feed the model an
 * unknown category.
 */

export const DEGREE_OPTIONS = [
  'B.Tech', 'B.E.', 'B.Sc', 'B.Com', 'BBA', 'BCA', 'B.A.',
  'M.Tech', 'M.Sc', 'M.Com', 'MBA', 'MCA', 'Other',
];

export const MAJOR_OPTIONS = [
  'Computer Science', 'Information Technology', 'Artificial Intelligence',
  'Data Science', 'Electronics', 'Mechanical Engineering',
  'Civil Engineering', 'Electrical Engineering', 'Business Administration',
  'Finance', 'Accounting', 'Commerce', 'Marketing', 'Economics',
  'Human Resources', 'Psychology', 'Mathematics', 'Physics', 'Other',
];

export const SKILL_SUGGESTIONS = [
  // Technical
  'Python', 'Java', 'JavaScript', 'SQL', 'Data Structures',
  'Machine Learning', 'Data Analysis', 'HTML', 'CSS', 'React',
  'Excel', 'Cloud Computing',
  // Non-technical
  'Communication', 'Leadership', 'Business Analysis', 'Financial Analysis',
  'Marketing', 'Sales', 'Project Management', 'Human Resources',
  'Documentation', 'Negotiation', 'Research',
];

export const CERTIFICATION_OPTIONS = [
  'AWS Certified Cloud Practitioner', 'Microsoft Azure Fundamentals',
  'Google Data Analytics', 'Oracle Java Certification', 'Cisco CCNA',
  'NPTEL Certification',
];

export const COMMUNICATION_OPTIONS = ['Beginner', 'Intermediate', 'Advanced'];

export const INTERNSHIP_OPTIONS = [
  { value: 'Yes', label: 'Yes — I have internship experience' },
  { value: 'No', label: 'No — not yet' },
];

/** The nine model fields, in contract order. */
export const PROFILE_FIELDS = [
  'degree', 'major', 'cgpa', 'skills', 'certification_name',
  'internship_experience', 'projects_count', 'communication_skill',
  'experience_months',
];

export const EMPTY_PROFILE = {
  degree: '',
  major: '',
  cgpa: '',
  skills: [],
  certification_name: '',
  internship_experience: '',
  projects_count: '',
  communication_skill: '',
  experience_months: '',
};

/** Count filled fields (0–9) for the profile-completion indicator. */
export function profileCompletion(profile) {
  if (!profile) return 0;
  let filled = 0;
  if (profile.degree) filled += 1;
  if (profile.major) filled += 1;
  if (profile.cgpa !== '' && profile.cgpa !== null && profile.cgpa !== undefined) filled += 1;
  if (Array.isArray(profile.skills) && profile.skills.length > 0) filled += 1;
  if (profile.certification_name) filled += 1;
  if (profile.internship_experience) filled += 1;
  if (profile.projects_count !== '' && profile.projects_count !== null && profile.projects_count !== undefined) filled += 1;
  if (profile.communication_skill) filled += 1;
  if (profile.experience_months !== '' && profile.experience_months !== null && profile.experience_months !== undefined) filled += 1;
  return filled;
}
