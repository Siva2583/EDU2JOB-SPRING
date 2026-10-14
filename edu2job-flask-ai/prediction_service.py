"""Model loading and prediction adapter for the NEW EDU2JOB 2.0 model.

Design rules (non-negotiable):
  * Only artifacts inside ``MODEL_DIR`` (default ``ml_models/v2/``) are ever
    loaded. The old v1 single-label model that lives directly in
    ``ml_models/`` is never touched — there is no silent fallback.
  * If the new artifacts are missing or unloadable, the service reports
    NOT READY and /predict returns 503. No dummy model, no fabricated roles.
  * Scores are returned only when the estimator genuinely produces
    probabilities. Rank-only decodings return ``score: None``.

How the MultiOutput target is decoded
-------------------------------------
The new target is ``top_5_job_roles`` over 128 job-role labels, but "how"
those five roles are encoded depends on the (not yet supplied) training
code. This adapter therefore supports the realistic encodings and picks
one deterministically:

  1. ``model_metadata.json`` may declare ``decode_mode`` explicitly
     (``single_label`` | ``multi_label`` | ``rank_positions``). This is the
     preferred, unambiguous path once the real artifacts arrive.
  2. Otherwise the shape of ``predict_proba`` output decides:
       - one (n, n_classes) matrix  -> single-label classification:
         rank classes by probability, score = probability.
       - a list of per-output (n, 2) matrices -> multi-label indicators:
         rank outputs by P(positive), score = that probability.
  3. Without probabilities, ``predict`` output is decoded as:
       - a binary indicator matrix (columns = labels) -> positive labels,
         in training column order, score = None;
       - a (n, 5) matrix of label codes ONLY when metadata declares
         ``decode_mode: "rank_positions"`` (position i = i-th best role).
         This is never guessed, because a bare predict() cannot be told
         apart from other encodings reliably.

Label names come from, in order: ``job_role_labels.json`` /
a label-encoder pickle in MODEL_DIR, the estimator's own string
``classes_``, or metadata ``labels``. If role names cannot be recovered,
the bundle is NOT READY and the health endpoint says exactly what is
missing.
"""
import json
import pickle
from pathlib import Path

import config
from job_scopes import JOB_SCOPES

STATUS_READY = "READY"
STATUS_NOT_CONFIGURED = "NOT_CONFIGURED"
STATUS_LOAD_ERROR = "LOAD_ERROR"


class ModelNotReadyError(Exception):
    """Raised when inference is requested before the new model is ready."""


class InvalidModelOutputError(Exception):
    """Raised when the estimator's output cannot be decoded honestly."""


def _load_pickle(path):
    with open(path, "rb") as handle:
        return pickle.load(handle)


def _load_labels(labels_path, metadata):
    """Return (labels, source_description) or (None, None)."""
    if metadata.get("labels"):
        labels = [str(x) for x in metadata["labels"]]
        return labels, "model_metadata.json:labels"

    if labels_path is not None and labels_path.exists():
        if labels_path.suffix == ".json":
            data = json.loads(labels_path.read_text(encoding="utf-8"))
            if isinstance(data, dict):
                data = data.get("labels") or data.get("classes") or []
            return [str(x) for x in data], labels_path.name
        obj = _load_pickle(labels_path)
        classes = getattr(obj, "classes_", None)
        if classes is not None:
            return [str(x) for x in list(classes)], labels_path.name
        if isinstance(obj, (list, tuple)):
            return [str(x) for x in obj], labels_path.name
    return None, None


def _find_aux_file(model_dir, configured_name, alternatives):
    candidate = model_dir / configured_name
    if candidate.exists():
        return candidate
    for name in alternatives:
        candidate = model_dir / name
        if candidate.exists():
            return candidate
    return None


class ModelBundle:
    """Loads and wraps the new model artifacts; decodes ranked top-5 roles."""

    def __init__(self, model_dir=None):
        self.model_dir = Path(model_dir) if model_dir else Path(config.MODEL_DIR)
        self.model_version = config.MODEL_VERSION
        self.status = STATUS_NOT_CONFIGURED
        self.detail = ""
        self.missing = []
        self.metadata = {}
        self.estimator = None
        self.preprocessor = None
        self.labels = None
        self.labels_source = None
        self._use_preprocessor = None  # learned on first predict
        self._load()

    # ------------------------------------------------------------------
    # Loading
    # ------------------------------------------------------------------
    def _load(self):
        model_path = self.model_dir / config.MODEL_FILENAME
        if not self.model_dir.exists() or not model_path.exists():
            self.status = STATUS_NOT_CONFIGURED
            self.missing = [f"{self.model_dir.name}/{config.MODEL_FILENAME}"]
            self.detail = (
                "New EDU2JOB 2.0 model artifacts are not installed yet. "
                f"Expected the trained estimator at {model_path}. "
                "See ml_models/README.md for the required files."
            )
            return

        try:
            metadata_path = self.model_dir / config.METADATA_FILENAME
            if metadata_path.exists():
                self.metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
                if self.metadata.get("model_version"):
                    self.model_version = str(self.metadata["model_version"])

            self.estimator = self._load_artifact(
                self.metadata.get("estimator_file") or config.MODEL_FILENAME)

            preprocessor_name = self.metadata.get("preprocessor_file") or config.PREPROCESSOR_FILENAME
            preprocessor_path = _find_aux_file(
                self.model_dir, preprocessor_name,
                ["preprocessor.pkl", "preprocessing.pkl", "encoder.pkl", "encoders.pkl"])
            if preprocessor_path is not None:
                self.preprocessor = _load_pickle(preprocessor_path)

            labels_path = None
            labels_name = self.metadata.get("labels_file") or config.LABELS_FILENAME
            labels_path = _find_aux_file(
                self.model_dir, labels_name,
                ["job_role_labels.json", "labels.json", "label_encoder.pkl",
                 "labels.pkl", "target_encoder.pkl", "mlb_encoder.pkl",
                 "label_binarizer.pkl"])
            self.labels, self.labels_source = _load_labels(labels_path, self.metadata)

            if self.labels is None:
                classes = getattr(self.estimator, "classes_", None)
                if classes is not None and len(classes) > 0 and isinstance(classes[0], str):
                    self.labels = [str(c) for c in classes]
                    self.labels_source = "estimator.classes_"

            decode_mode = self.metadata.get("decode_mode")
            needs_labels = decode_mode in ("multi_label", "rank_positions") or (
                decode_mode is None and not hasattr(self.estimator, "predict_proba"))
            if self.labels is None and (needs_labels or hasattr(self.estimator, "estimators_")):
                self.status = STATUS_LOAD_ERROR
                self.missing = ["job_role_labels.json (or label_encoder.pkl / metadata labels)"]
                self.detail = (
                    "Model estimator loaded, but the job-role label mapping is "
                    "missing, so predicted codes cannot be turned into role "
                    "names honestly. Supply the label artifact described in "
                    "ml_models/README.md."
                )
                self.estimator = None
                return

            self.status = STATUS_READY
            self.detail = (
                f"Loaded estimator from {model_path.name}"
                + (f", preprocessor from {preprocessor_path.name}" if self.preprocessor is not None else "")
                + (f", {len(self.labels)} role labels from {self.labels_source}" if self.labels else "")
                + "."
            )
        except Exception as exc:  # never leak internals to clients; log detail here
            self.status = STATUS_LOAD_ERROR
            self.detail = f"Failed to load new model artifacts: {type(exc).__name__}"
            self.estimator = None

    def _load_artifact(self, filename):
        path = self.model_dir / filename
        try:
            import joblib
            return joblib.load(path)
        except ImportError:
            return _load_pickle(path)

    # ------------------------------------------------------------------
    # Health
    # ------------------------------------------------------------------
    @property
    def ready(self):
        return self.status == STATUS_READY and self.estimator is not None

    def health(self):
        return {
            "status": "ok" if self.ready else "degraded",
            "service": "edu2job-flask-ai",
            "modelReady": self.ready,
            "modelStatus": self.status,
            "modelVersion": self.model_version,
            "modelDir": self.model_dir.name,
            "labelsLoaded": len(self.labels) if self.labels else 0,
            "missingArtifacts": self.missing,
            "detail": self.detail,
        }

    def vocabularies(self):
        """Authoritative category vocabularies from metadata, if supplied."""
        vocab = self.metadata.get("category_values") or self.metadata.get("vocabularies") or {}
        result = dict(vocab) if isinstance(vocab, dict) else {}
        if self.metadata.get("skill_vocabulary"):
            result["skills"] = self.metadata["skill_vocabulary"]
        return result

    # ------------------------------------------------------------------
    # Inference
    # ------------------------------------------------------------------
    def predict_top_roles(self, cleaned):
        """Return up to five {"rank", "role", "score", "scope"} dicts."""
        if not self.ready:
            raise ModelNotReadyError(self.detail)

        import pandas as pd  # local import keeps /health cheap without pandas

        raw_frame = pd.DataFrame([{
            "degree": cleaned["degree"],
            "major": cleaned["major"],
            "cgpa": cleaned["cgpa"],
            "skills": cleaned["skills"],
            "certification_name": cleaned["certification_name"],
            "internship_experience": cleaned["internship_experience"],
            "projects_count": cleaned["projects_count"],
            "communication_skill": cleaned["communication_skill"],
            "experience_months": cleaned["experience_months"],
        }])

        decode_mode = self.metadata.get("decode_mode")
        ranked = None

        if hasattr(self.estimator, "predict_proba"):
            ranked = self._decode_proba(raw_frame, decode_mode)
        if ranked is None:
            ranked = self._decode_predict(raw_frame, decode_mode)
        if ranked is None:
            raise InvalidModelOutputError(
                "The model's output encoding could not be determined. "
                "Add decode_mode (single_label | multi_label | rank_positions) "
                "to model_metadata.json — see ml_models/README.md."
            )

        predictions = []
        for rank, (role, score) in enumerate(ranked[: config.MAX_RECOMMENDATIONS], start=1):
            entry = {"rank": rank, "role": role, "score": score}
            scope = JOB_SCOPES.get(role)
            if scope:
                entry["scope"] = scope
            predictions.append(entry)
        if not predictions:
            raise InvalidModelOutputError("The model returned no usable job roles.")
        return predictions

    # -- input plumbing: raw frame vs preprocessor-encoded frame --------
    def _call(self, method_name, raw_frame):
        """Call estimator method, routing through the preprocessor if fitted so."""
        method = getattr(self.estimator, method_name)
        if self._use_preprocessor is True:
            return method(self.preprocessor.transform(raw_frame))
        try:
            result = method(raw_frame)
            if self.preprocessor is not None:
                self._use_preprocessor = False
            return result
        except Exception:
            if self.preprocessor is None:
                raise
            result = method(self.preprocessor.transform(raw_frame))
            self._use_preprocessor = True
            return result

    # -- decoders ---------------------------------------------------------
    def _decode_proba(self, raw_frame, decode_mode):
        try:
            proba = self._call("predict_proba", raw_frame)
        except Exception as exc:
            raise InvalidModelOutputError(f"predict_proba failed: {type(exc).__name__}") from exc

        import numpy as np

        # Multi-output: list of per-output probability arrays.
        if isinstance(proba, (list, tuple)):
            scores = []
            for output_proba in proba:
                arr = np.asarray(output_proba, dtype=float)
                if arr.ndim != 2 or arr.shape[0] == 0:
                    raise InvalidModelOutputError("Unexpected per-output probability shape.")
                # Binary output: P(positive class). Multi-class rank head:
                # probability of the output's top class is not comparable
                # across heads, so prefer binary interpretation; for wider
                # heads fall back to max-probability as the head's score.
                scores.append(float(arr[0, 1]) if arr.shape[1] == 2 else float(arr[0].max()))
            labels = self._require_labels(len(scores))
            order = sorted(range(len(scores)), key=lambda i: scores[i], reverse=True)
            return [(labels[i], scores[i]) for i in order]

        arr = np.asarray(proba, dtype=float)
        if arr.ndim == 2 and arr.shape[0] >= 1:
            row = arr[0]
            labels = self._labels_for_single_label(row)
            order = sorted(range(len(row)), key=lambda i: row[i], reverse=True)
            return [(labels[i], float(row[i])) for i in order]
        return None

    def _decode_predict(self, raw_frame, decode_mode):
        try:
            output = self._call("predict", raw_frame)
        except Exception as exc:
            raise InvalidModelOutputError(f"predict failed: {type(exc).__name__}") from exc

        import numpy as np

        arr = np.asarray(output)
        if arr.ndim == 1:
            arr = arr.reshape(1, -1)
        if arr.ndim != 2 or arr.shape[0] == 0:
            return None
        row = arr[0]

        if decode_mode == "rank_positions":
            labels = self._require_labels(None)
            roles = []
            for value in row:
                role = self._label_for_code(value, labels)
                if role is not None and role not in roles:
                    roles.append(role)
            return [(role, None) for role in roles]

        # Binary indicator matrix: columns correspond to labels.
        unique = set(np.unique(arr).tolist())
        if unique.issubset({0, 1, 0.0, 1.0, True, False}) and len(row) > 1:
            labels = self._require_labels(len(row))
            positives = [labels[i] for i, v in enumerate(row) if v in (1, 1.0, True)]
            # Without probabilities there is no honest ranking signal, so
            # positives keep their training column order and score=None.
            return [(role, None) for role in positives]

        # Single scalar prediction: one role, no ranking available.
        if len(row) == 1:
            labels = self.labels
            role = self._label_for_code(row[0], labels) if labels else str(row[0])
            return [(role, None)]
        return None

    # -- label helpers ------------------------------------------------------
    def _require_labels(self, expected_count):
        if not self.labels:
            raise InvalidModelOutputError(
                "Job-role label mapping is missing; cannot decode model output."
            )
        if expected_count is not None and len(self.labels) != expected_count:
            raise InvalidModelOutputError(
                f"Label mapping has {len(self.labels)} entries but the model "
                f"produced {expected_count} outputs."
            )
        return self.labels

    def _labels_for_single_label(self, proba_row):
        classes = getattr(self.estimator, "classes_", None)
        if classes is not None and len(classes) == len(proba_row):
            return [str(c) if not (self.labels and isinstance(c, (int, float)))
                    else self._label_for_code(c, self.labels) or str(c)
                    for c in classes]
        return self._require_labels(len(proba_row))

    @staticmethod
    def _label_for_code(code, labels):
        if labels is None:
            return None
        if isinstance(code, str):
            return code if code in labels else None
        try:
            index = int(code)
        except (TypeError, ValueError):
            return None
        return labels[index] if 0 <= index < len(labels) else None


# Module-level bundle used by the Flask app. Loading failures are captured
# in the bundle state (never raised at import time), so /health can explain
# exactly what is missing while /predict honestly returns 503.
BUNDLE = None


def get_bundle():
    global BUNDLE
    if BUNDLE is None:
        BUNDLE = ModelBundle()
    return BUNDLE
