"""Flask API tests for the EDU2JOB 2.0 nine-field schema.

A fake estimator stands in for the real MultiOutput Random Forest so the
HTTP + decoding behaviour is testable before the genuine artifacts arrive.
These are ADAPTER tests, not model-quality tests: no real inference is
claimed anywhere in this file.
"""
import sys
from pathlib import Path

import numpy as np
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app import create_app  # noqa: E402
from prediction_service import ModelBundle  # noqa: E402

VALID_PAYLOAD = {
    "degree": "B.Tech",
    "major": "Computer Science",
    "cgpa": 8.5,
    "skills": ["Python", "SQL", "Java"],
    "certification_name": None,
    "internship_experience": "Yes",
    "projects_count": 3,
    "communication_skill": "Advanced",
    "experience_months": 6,
}

ROLES = ["Data Analyst", "Software Engineer", "Business Analyst",
         "Accountant", "HR Executive", "Data Scientist"]


class FakeMultiOutputForest:
    """Mimics MultiOutputClassifier.predict_proba: one (n,2) array/output."""

    def predict_proba(self, frame):
        # Role probabilities deliberately unsorted to prove ranking works.
        positive = [0.10, 0.90, 0.40, 0.05, 0.70, 0.20]
        return [np.array([[1 - p, p]]) for p in positive]


class FakeSingleLabelPipeline:
    classes_ = np.array(ROLES)

    def predict_proba(self, frame):
        return np.array([[0.05, 0.55, 0.10, 0.02, 0.25, 0.03]])


class FakeRankPositionsModel:
    """predict() returns label codes ordered best-first (5 rank slots)."""

    def predict(self, frame):
        return np.array([[1, 4, 2, 0, 5]])


def _ready_bundle(estimator, labels=ROLES, metadata=None):
    bundle = ModelBundle.__new__(ModelBundle)
    bundle.model_dir = Path("test-model-dir")
    bundle.model_version = "edu2job-v2-test"
    bundle.status = "READY"
    bundle.detail = "test bundle"
    bundle.missing = []
    bundle.metadata = metadata or {}
    bundle.estimator = estimator
    bundle.preprocessor = None
    bundle.labels = labels
    bundle.labels_source = "test"
    bundle._use_preprocessor = None
    return bundle


@pytest.fixture()
def multioutput_client():
    return create_app(_ready_bundle(FakeMultiOutputForest())).test_client()


def test_health_reports_ready(multioutput_client):
    response = multioutput_client.get("/health")
    assert response.status_code == 200
    body = response.get_json()
    assert body["modelReady"] is True
    assert body["labelsLoaded"] == len(ROLES)


def test_valid_prediction_is_ranked_top_five(multioutput_client):
    response = multioutput_client.post("/predict", json=VALID_PAYLOAD)
    assert response.status_code == 200
    body = response.get_json()
    roles = [p["role"] for p in body["predictions"]]
    assert roles[0] == "Software Engineer"   # p=0.90
    assert roles[1] == "HR Executive"        # p=0.70
    assert len(roles) == 5
    assert body["predictions"][0]["score"] == pytest.approx(0.90)
    assert body["modelVersion"] == "edu2job-v2-test"


def test_single_label_proba_decoding():
    client = create_app(_ready_bundle(FakeSingleLabelPipeline(), labels=None)).test_client()
    response = client.post("/predict", json=VALID_PAYLOAD)
    assert response.status_code == 200
    roles = [p["role"] for p in response.get_json()["predictions"]]
    assert roles[:2] == ["Software Engineer", "HR Executive"]


def test_rank_positions_decoding_only_via_metadata():
    bundle = _ready_bundle(FakeRankPositionsModel(), metadata={"decode_mode": "rank_positions"})
    client = create_app(bundle).test_client()
    response = client.post("/predict", json=VALID_PAYLOAD)
    assert response.status_code == 200
    predictions = response.get_json()["predictions"]
    assert [p["role"] for p in predictions][:3] == [
        "Software Engineer", "HR Executive", "Business Analyst"]
    assert predictions[0]["score"] is None  # no fabricated confidence


def test_missing_fields_return_400(multioutput_client):
    response = multioutput_client.post("/predict", json={"degree": "B.Tech"})
    assert response.status_code == 400
    fields = response.get_json()["error"]["fields"]
    for expected in ("major", "cgpa", "skills", "internship_experience",
                     "projects_count", "communication_skill", "experience_months"):
        assert expected in fields


def test_invalid_values_return_400(multioutput_client):
    bad = dict(VALID_PAYLOAD, cgpa=11.5, projects_count=-1)
    response = multioutput_client.post("/predict", json=bad)
    assert response.status_code == 400

    bad = dict(VALID_PAYLOAD, internship_experience="Maybe")
    assert multioutput_client.post("/predict", json=bad).status_code == 400


def test_unknown_skill_rejected_when_vocabulary_known():
    bundle = _ready_bundle(
        FakeMultiOutputForest(),
        metadata={"category_values": {"skills": ["Python", "SQL"]}})
    client = create_app(bundle).test_client()
    payload = dict(VALID_PAYLOAD, skills=["Python", "Underwater Basket Weaving"])
    response = client.post("/predict", json=payload)
    assert response.status_code == 400
    assert "Underwater Basket Weaving" in response.get_json()["error"]["fields"]["skills"]


def test_missing_artifacts_report_not_ready(tmp_path):
    bundle = ModelBundle(model_dir=tmp_path / "empty-v2")
    assert bundle.ready is False
    client = create_app(bundle).test_client()

    health = client.get("/health")
    assert health.status_code == 503
    assert health.get_json()["modelReady"] is False

    response = client.post("/predict", json=VALID_PAYLOAD)
    assert response.status_code == 503
    assert response.get_json()["error"]["code"] == "MODEL_NOT_READY"


def test_malformed_json_returns_400(multioutput_client):
    response = multioutput_client.post(
        "/predict", data="{not json", content_type="application/json")
    assert response.status_code == 400
