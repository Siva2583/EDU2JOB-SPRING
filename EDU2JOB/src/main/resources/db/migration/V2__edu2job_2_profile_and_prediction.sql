-- ============================================================
-- EDU2JOB 2.0 — manual schema migration (OPTIONAL)
--
-- The application runs with spring.jpa.hibernate.ddl-auto=update, which
-- adds these same nullable columns automatically on first boot. Run this
-- script instead only if you prefer to manage the schema by hand (then
-- consider switching ddl-auto to 'validate' afterwards).
--
-- Safe by design:
--   * no table is dropped or recreated;
--   * every new column is NULLABLE, so existing users, profiles and
--     prediction history keep working untouched;
--   * old rows simply have NULL in the new fields and a NULL
--     model_version, which the API/UI render as "legacy (old model)";
--   * year_of_graduation is kept for legacy rows and no longer written.
-- ============================================================

ALTER TABLE user_prediction_history
    ADD COLUMN IF NOT EXISTS certification_name   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS internship_experience VARCHAR(50),
    ADD COLUMN IF NOT EXISTS projects_count        INTEGER,
    ADD COLUMN IF NOT EXISTS communication_skill   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS experience_months     INTEGER;

ALTER TABLE pred_history
    ADD COLUMN IF NOT EXISTS certification_name   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS internship_experience VARCHAR(50),
    ADD COLUMN IF NOT EXISTS projects_count        INTEGER,
    ADD COLUMN IF NOT EXISTS communication_skill   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS experience_months     INTEGER,
    ADD COLUMN IF NOT EXISTS model_version         VARCHAR(50);

-- Notes:
-- * cgpa was and remains numeric(4,2) — precision up to 10.00 covers the
--   0–10 scale exactly, so no type change is needed.
-- * skills stays TEXT. New rows store a JSON array string
--   (e.g. '["Python","SQL"]'); old comma-joined rows are still parsed
--   correctly by the application (see SkillsJson). No data rewrite needed.
-- * users is intentionally untouched: accounts, ids and BCrypt password
--   hashes are preserved.
