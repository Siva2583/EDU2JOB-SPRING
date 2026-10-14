"""Validation and normalization for the nine-field EDU2JOB 2.0 schema.

The canonical field names (snake_case) are shared verbatim by React,
Spring Boot and this service:

    degree, major, cgpa, skills, certification_name,
    internship_experience, projects_count, communication_skill,
    experience_months

IMPORTANT — category vocabularies: the definitive lists of supported
degrees, majors, skills, certifications and category spellings live in the
training data / fitted preprocessing artifacts, which are supplied with the
new model (optionally described in ml_models/v2/model_metadata.json). The
lists below are PROVISIONAL fallbacks used only for light normalization
(e.g. mapping "yes" -> "Yes") until the real vocabularies arrive. When the
model metadata provides vocabularies, those are authoritative and unknown
values are rejected with a clear 400 instead of being silently dropped.
"""

# --- Provisional fallback vocabularies (CONFIRM AGAINST THE DATASET) ---
DEGREES = [
    "B.Tech", "B.E.", "B.Sc", "B.Com", "BBA", "BCA", "B.A.",
    "M.Tech", "M.Sc", "M.Com", "MBA", "MCA", "Other",
]
MAJORS = [
    "Computer Science", "Information Technology", "Artificial Intelligence",
    "Data Science", "Electronics", "Mechanical Engineering",
    "Civil Engineering", "Electrical Engineering", "Business Administration",
    "Finance", "Accounting", "Commerce", "Marketing", "Economics",
    "Human Resources", "Psychology", "Mathematics", "Physics", "Other",
]
CERTIFICATION_EXAMPLES = [
    "AWS Certified Cloud Practitioner", "Microsoft Azure Fundamentals",
    "Google Data Analytics", "Oracle Java Certification", "Cisco CCNA",
    "NPTEL Certification",
]
COMMUNICATION_LEVELS = ["Beginner", "Intermediate", "Advanced"]
INTERNSHIP_VALUES = ["Yes", "No"]

FIELDS = [
    "degree", "major", "cgpa", "skills", "certification_name",
    "internship_experience", "projects_count", "communication_skill",
    "experience_months",
]


def _case_insensitive_match(value, options):
    lowered = value.strip().lower()
    for option in options:
        if option.lower() == lowered:
            return option
    return None


def _as_non_negative_int(value):
    """Return (int_value, error_message). Booleans are not integers here."""
    if isinstance(value, bool):
        return None, "must be a non-negative integer"
    if isinstance(value, int):
        result = value
    elif isinstance(value, float) and value.is_integer():
        result = int(value)
    elif isinstance(value, str) and value.strip().lstrip("+-").isdigit():
        result = int(value.strip())
    else:
        return None, "must be a non-negative integer"
    if result < 0:
        return None, "must be a non-negative integer"
    return result, None


def validate_payload(data, vocabularies=None):
    """Validate a raw JSON body against the nine-field schema.

    Args:
        data: parsed JSON body (dict expected).
        vocabularies: optional dict from model_metadata.json, e.g.
            {"degree": [...], "major": [...], "skills": [...],
             "certification_name": [...], "communication_skill": [...],
             "internship_experience": [...]}
            When a vocabulary is present it is enforced; unknown skills are
            reported, never silently discarded.

    Returns:
        (cleaned, errors): ``cleaned`` is the normalized nine-field dict
        (certification_name may be None); ``errors`` maps field -> message.
    """
    vocabularies = vocabularies or {}
    if not isinstance(data, dict):
        return None, {"body": "JSON object expected"}

    errors = {}
    cleaned = {}

    # --- degree / major -------------------------------------------------
    for field in ("degree", "major"):
        value = data.get(field)
        if value is None or (isinstance(value, str) and not value.strip()):
            errors[field] = f"{field} is required"
            continue
        if not isinstance(value, str):
            errors[field] = f"{field} must be a string"
            continue
        value = value.strip()
        vocab = vocabularies.get(field)
        if vocab:
            match = _case_insensitive_match(value, vocab)
            if match is None:
                errors[field] = f"Unsupported {field}: '{value}'"
                continue
            value = match
        cleaned[field] = value

    # --- cgpa ------------------------------------------------------------
    cgpa = data.get("cgpa")
    if cgpa is None or (isinstance(cgpa, str) and not cgpa.strip()):
        errors["cgpa"] = "cgpa is required"
    elif isinstance(cgpa, bool):
        errors["cgpa"] = "cgpa must be a number between 0 and 10"
    else:
        try:
            cgpa_value = float(cgpa)
            if cgpa_value != cgpa_value or cgpa_value in (float("inf"), float("-inf")):
                raise ValueError
            if not 0.0 <= cgpa_value <= 10.0:
                errors["cgpa"] = "cgpa must be between 0 and 10 (10-point scale)"
            else:
                cleaned["cgpa"] = cgpa_value
        except (TypeError, ValueError):
            errors["cgpa"] = "cgpa must be a number between 0 and 10"

    # --- skills ----------------------------------------------------------
    skills = data.get("skills")
    if skills is None:
        errors["skills"] = "skills is required"
    elif isinstance(skills, str):
        # Tolerate a comma-joined string from older clients; the contract
        # type is a JSON array and new clients always send one.
        skills_list = [s.strip() for s in skills.split(",") if s.strip()]
        if not skills_list:
            errors["skills"] = "at least one skill is required"
        else:
            cleaned["skills"] = skills_list
    elif isinstance(skills, list):
        skills_list = []
        for item in skills:
            if not isinstance(item, str) or not item.strip():
                errors["skills"] = "skills must be an array of non-empty strings"
                break
            skills_list.append(item.strip())
        else:
            if not skills_list:
                errors["skills"] = "at least one skill is required"
            else:
                cleaned["skills"] = skills_list
    else:
        errors["skills"] = "skills must be an array of strings"

    if "skills" in cleaned and vocabularies.get("skills"):
        known = {s.lower(): s for s in vocabularies["skills"]}
        unknown = [s for s in cleaned["skills"] if s.lower() not in known]
        if unknown:
            errors["skills"] = (
                "Unknown skill(s) not supported by the model: "
                + ", ".join(unknown)
            )
        else:
            cleaned["skills"] = [known[s.lower()] for s in cleaned["skills"]]

    # --- certification_name (optional; empty means "none") ---------------
    certification = data.get("certification_name")
    if certification is None or (isinstance(certification, str) and not certification.strip()):
        cleaned["certification_name"] = None
    elif not isinstance(certification, str):
        errors["certification_name"] = "certification_name must be a string or null"
    else:
        value = certification.strip()
        if value.lower() in ("none", "no", "n/a", "-"):
            cleaned["certification_name"] = None
        else:
            vocab = vocabularies.get("certification_name")
            if vocab:
                match = _case_insensitive_match(value, vocab)
                if match is None:
                    errors["certification_name"] = (
                        f"Unsupported certification_name: '{value}'"
                    )
                else:
                    cleaned["certification_name"] = match
            else:
                cleaned["certification_name"] = value

    # --- internship_experience -------------------------------------------
    internship = data.get("internship_experience")
    if internship is None or (isinstance(internship, str) and not internship.strip()):
        errors["internship_experience"] = "internship_experience is required"
    elif isinstance(internship, bool):
        cleaned["internship_experience"] = "Yes" if internship else "No"
    elif isinstance(internship, str):
        options = vocabularies.get("internship_experience") or INTERNSHIP_VALUES
        match = _case_insensitive_match(internship, options)
        if match is None and vocabularies.get("internship_experience"):
            errors["internship_experience"] = (
                f"Unsupported internship_experience: '{internship.strip()}'"
            )
        elif match is None:
            lowered = internship.strip().lower()
            if lowered in ("true", "1"):
                cleaned["internship_experience"] = "Yes"
            elif lowered in ("false", "0"):
                cleaned["internship_experience"] = "No"
            else:
                errors["internship_experience"] = (
                    "internship_experience must be 'Yes' or 'No'"
                )
        else:
            cleaned["internship_experience"] = match
    else:
        errors["internship_experience"] = "internship_experience must be 'Yes' or 'No'"

    # --- projects_count / experience_months -------------------------------
    for field in ("projects_count", "experience_months"):
        value = data.get(field)
        if value is None or (isinstance(value, str) and not str(value).strip()):
            errors[field] = f"{field} is required"
            continue
        int_value, error = _as_non_negative_int(value)
        if error:
            errors[field] = f"{field} {error}"
        else:
            cleaned[field] = int_value

    # --- communication_skill ----------------------------------------------
    communication = data.get("communication_skill")
    if communication is None or (isinstance(communication, str) and not communication.strip()):
        errors["communication_skill"] = "communication_skill is required"
    elif not isinstance(communication, str):
        errors["communication_skill"] = "communication_skill must be a string"
    else:
        options = vocabularies.get("communication_skill") or COMMUNICATION_LEVELS
        match = _case_insensitive_match(communication, options)
        if match is not None:
            cleaned["communication_skill"] = match
        elif vocabularies.get("communication_skill"):
            errors["communication_skill"] = (
                f"Unsupported communication_skill: '{communication.strip()}'"
            )
        else:
            cleaned["communication_skill"] = communication.strip()

    if errors:
        return None, errors
    # Guarantee exactly the nine contract fields, in schema order.
    return {field: cleaned.get(field) for field in FIELDS}, {}
