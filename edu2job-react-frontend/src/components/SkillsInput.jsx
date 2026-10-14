import React, { useState } from 'react';
import { SKILL_SUGGESTIONS } from '../constants/profileOptions';

/**
 * Multi-value skills editor: type + Enter/comma to add, click suggestions,
 * × to remove. Works for technical and non-technical skills alike. Unknown
 * (non-suggested) skills are kept and sent as typed — if the installed
 * model vocabulary rejects one, the API error names it explicitly.
 */
function SkillsInput({ value = [], onChange, disabled = false, id = 'skills-input' }) {
  const [draft, setDraft] = useState('');

  const addSkill = (raw) => {
    const skill = String(raw || '').trim().replace(/,$/, '');
    if (!skill) return;
    if (value.some((s) => s.toLowerCase() === skill.toLowerCase())) {
      setDraft('');
      return;
    }
    onChange([...value, skill]);
    setDraft('');
  };

  const removeSkill = (skill) => onChange(value.filter((s) => s !== skill));

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      addSkill(draft);
    } else if (e.key === 'Backspace' && !draft && value.length > 0) {
      removeSkill(value[value.length - 1]);
    }
  };

  const suggestions = SKILL_SUGGESTIONS.filter(
    (s) => !value.some((v) => v.toLowerCase() === s.toLowerCase())
      && (!draft || s.toLowerCase().includes(draft.toLowerCase()))
  ).slice(0, 8);

  return (
    <div className="skills-input">
      <div className="skills-chips">
        {value.map((skill) => (
          <span key={skill} className="skill-chip">
            {skill}
            {!disabled && (
              <button type="button" aria-label={`Remove ${skill}`}
                onClick={() => removeSkill(skill)}>×</button>
            )}
          </span>
        ))}
        <input
          id={id}
          type="text"
          value={draft}
          disabled={disabled}
          placeholder={value.length ? 'Add another skill…' : 'e.g. Python, Communication, Excel'}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={handleKeyDown}
          onBlur={() => addSkill(draft)}
          aria-label="Add a skill"
        />
      </div>
      {!disabled && suggestions.length > 0 && (
        <div className="skills-suggestions" aria-label="Suggested skills">
          {suggestions.map((s) => (
            <button type="button" key={s} className="suggestion-chip"
              onMouseDown={(e) => { e.preventDefault(); addSkill(s); }}>
              + {s}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default SkillsInput;
