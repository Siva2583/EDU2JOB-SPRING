"""Runtime configuration for the EDU2JOB 2.0 Flask ML service.

Everything environment-specific is read from environment variables so the
same code runs locally and in deployment. See .env.example.
"""
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent

# Directory holding the NEW model artifacts. The user supplies the trained
# files here (see ml_models/README.md). The OLD v1 artifacts that live
# directly under ml_models/ are never loaded by this service.
MODEL_DIR = Path(os.environ.get("MODEL_DIR", BASE_DIR / "ml_models" / "v2"))

# Canonical artifact filenames inside MODEL_DIR (overridable via metadata).
MODEL_FILENAME = os.environ.get("MODEL_FILE", "model.pkl")
PREPROCESSOR_FILENAME = os.environ.get("PREPROCESSOR_FILE", "preprocessor.pkl")
LABELS_FILENAME = os.environ.get("LABELS_FILE", "job_role_labels.json")
METADATA_FILENAME = os.environ.get("METADATA_FILE", "model_metadata.json")

# Reported in every prediction response and persisted by Spring Boot so
# new-model predictions stay distinguishable from legacy ones.
MODEL_VERSION = os.environ.get("MODEL_VERSION", "edu2job-v2")

HOST = os.environ.get("FLASK_HOST", "0.0.0.0")
PORT = int(os.environ.get("FLASK_PORT", "5000"))

# Sanity cap on the ranked list: the model recommends up to five roles.
MAX_RECOMMENDATIONS = 5
