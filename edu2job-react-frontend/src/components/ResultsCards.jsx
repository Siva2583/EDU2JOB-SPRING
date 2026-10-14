import React from 'react';

/**
 * Ranked recommendation cards (up to five).
 *
 * Honesty rules: a score bar/percentage is rendered ONLY when the model
 * returned a real numeric score; descriptions come from a separately
 * maintained career-info source (the API's `scope`), are labelled as such,
 * and nothing here invents salaries, hiring chances, or guarantees.
 */
function ResultsCards({ predictions = [], modelVersion }) {
  if (!predictions.length) return null;
  return (
    <div>
      <div className="results-grid">
        {predictions.map((pred, idx) => {
          const rank = pred.rank || idx + 1;
          const hasScore = typeof pred.score === 'number' && !Number.isNaN(pred.score);
          // Flask may send 0–1 probabilities; display as a percentage only then.
          const scorePct = hasScore && pred.score <= 1 ? pred.score * 100 : pred.score;
          return (
            <article key={`${rank}-${pred.role}`} className={`role-card ${rank === 1 ? 'top-role' : ''}`}>
              <div className="role-card-header">
                <span className="rank-badge" aria-label={`Rank ${rank}`}>#{rank}</span>
                {rank === 1 && <span className="top-match-badge">Top match</span>}
              </div>
              <h3>{pred.role}</h3>
              {hasScore ? (
                <div>
                  <div className="confidence-bar">
                    <div className="confidence-bar-inner"
                      style={{ width: `${Math.max(0, Math.min(100, scorePct)).toFixed(1)}%` }} />
                  </div>
                  <p className="score-label">Model score: {scorePct.toFixed(1)}%</p>
                </div>
              ) : (
                <p className="score-label">Ranked by the model (no confidence score provided)</p>
              )}
              {pred.scope && (
                <p className="role-scope">{pred.scope}</p>
              )}
            </article>
          );
        })}
      </div>
      <p className="results-note">
        Recommendations from model <strong>{modelVersion || 'edu2job-v2'}</strong>.
        Descriptions are general career information, not model output. Use this
        shortlist as a starting point — not a guarantee of hiring outcomes.
      </p>
    </div>
  );
}

export default ResultsCards;
