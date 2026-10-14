# EDU2JOB 2.0 — New Model Artifacts (READ THIS FIRST)

The Flask service loads **only** the NEW model from `ml_models/v2/`.
That folder is intentionally **empty right now** (just a `.gitkeep`) — the
application is fully migrated and will honestly report
`503 MODEL_NOT_READY` until the real artifacts are dropped in. It never
falls back to the old model and never fabricates predictions.

> ⚠️ The three files directly under `ml_models/` (`job_role_model.pkl`,
> `label_encoder.pkl`, `mlb_encoder.pkl`) are the **old v1 single-label
> model** (7 roles, 4 inputs). They are kept only so the old behaviour stays
> auditable. The 2.0 code never loads them. Do not copy them into `v2/`.

---

## 1. Required artifacts — drop these into `ml_models/v2/`

| # | File | Required? | Purpose |
|---|------|-----------|---------|
| 1 | `model.pkl` | **Yes** | The fitted estimator (the MultiOutput Random Forest `rf`, or a full sklearn `Pipeline` containing preprocessing + estimator). Serialized with joblib/pickle. |
| 2 | `model_metadata.json` | **Strongly recommended** | Declares how to decode the estimator (see §3). Without it the adapter infers decoding from `predict_proba` shapes, and `rank_positions` output can never be guessed safely. |
| 3 | `job_role_labels.json` | Yes, unless labels are embedded in the estimator/metadata | The 128 job-role names, in the exact training output order. JSON list of strings, or `{"labels": [...]}`. Alternatives accepted: `label_encoder.pkl`, `labels.pkl`, `target_encoder.pkl`, `mlb_encoder.pkl` (LabelEncoder / MultiLabelBinarizer with `classes_`). |
| 4 | `preprocessor.pkl` | Yes, if `model.pkl` is a bare estimator | The fitted preprocessing pipeline that turns the raw 9-field input into the 538 training features (encoders/scalers/binarizers). Skip this only if `model.pkl` is already a full Pipeline that accepts the raw 9 columns. Alternatives accepted: `preprocessing.pkl`, `encoder.pkl`, `encoders.pkl`. |
| 5 | Anything else the training code saved | If applicable | Any extra fitted object used at inference (e.g. a separate skills `MultiLabelBinarizer`) must either be inside `preprocessor.pkl`/`model.pkl` or added here and named in `model_metadata.json`. |

Why so many files? A bare `rf` variable holds only trees. Inference also
needs (a) the exact 9 → 538 feature transformation, (b) the mapping from
output columns/codes to the 128 role names, and (c) knowledge of how the
five target roles were encoded. Those live in the training notebook's
other fitted objects — export them all.

## 2. Where each file must be placed

```
edu2job-flask-ai/
└── ml_models/
    ├── job_role_model.pkl      ← OLD v1 model (untouched, never loaded)
    ├── label_encoder.pkl       ← OLD
    ├── mlb_encoder.pkl         ← OLD
    └── v2/                     ← PUT THE NEW FILES HERE
        ├── model.pkl
        ├── preprocessor.pkl        (if model.pkl is not a full Pipeline)
        ├── job_role_labels.json
        └── model_metadata.json
```

The directory is configurable with the `MODEL_DIR` environment variable,
and individual filenames with `MODEL_FILE`, `PREPROCESSOR_FILE`,
`LABELS_FILE`, `METADATA_FILE` (or inside `model_metadata.json`).

## 3. `model_metadata.json` — the decoding contract

The estimator is described as a *MultiOutput Random Forest* over the
target `top_5_job_roles`. `rf.predict()` alone does **not** define a
ranked top-5 — how the five roles were encoded during training decides
everything. Declare it explicitly:

```json
{
  "model_version": "edu2job-v2",
  "decode_mode": "multi_label",
  "estimator_file": "model.pkl",
  "preprocessor_file": "preprocessor.pkl",
  "labels_file": "job_role_labels.json",
  "training": {
    "dataset": "edu2job_dataset.csv",
    "rows": 10000,
    "n_features_after_preprocessing": 538,
    "n_labels": 128,
    "sklearn_version": "1.6.1",
    "python_version": "3.11"
  },
  "category_values": {
    "degree": ["... exact training categories ..."],
    "major": ["... exact training categories ..."],
    "communication_skill": ["... exact training categories ..."],
    "internship_experience": ["Yes", "No"]
  },
  "skill_vocabulary": ["... every skill token the encoder knows ..."]
}
```

Supported `decode_mode` values:

| Value | Training representation | How roles/scores are produced |
|-------|------------------------|-------------------------------|
| `single_label` | One categorical target (128 classes) | `predict_proba` ranked descending; `score` = class probability |
| `multi_label` | One binary column per role (128 outputs) | Per-output `predict_proba` P(role applies) ranked descending; `score` = that probability |
| `rank_positions` | Five outputs, each a role code, best-first | `predict` codes mapped through the label list in order; `score` = `null` (no honest score exists) |

If `decode_mode` is omitted, the adapter infers `single_label` vs
`multi_label` from the `predict_proba` output shape, and can decode a
binary `predict()` matrix without scores — but it will refuse (502) rather
than guess a `rank_positions` encoding.

When `category_values` / `skill_vocabulary` are present they are enforced:
unknown degrees, majors or skills return a clear `400` naming the
offending value instead of being silently dropped before the model.

## 4. How the Flask application loads the files

`prediction_service.ModelBundle` (loaded once at startup):

1. Reads `model_metadata.json` if present (version, file names, decode mode, vocabularies).
2. joblib-loads the estimator; loads the preprocessor and label mapping if present.
3. The raw request is built as a one-row DataFrame with exactly the nine
   contract columns. If the estimator rejects the raw frame and a
   preprocessor exists, features come from `preprocessor.transform(...)`.
4. Output is decoded per §3 into at most five `{rank, role, score, scope?}`
   entries. `scope` is static editorial text from `job_scopes.py`, never
   model output; `score` is `null` whenever the model gives no probability.

## 5. Required Python / scikit-learn versions

Pickled forests only load reliably with the scikit-learn version that
created them. The pinned `requirements.txt` uses:

```
Flask==3.0.3  pandas==2.2.2  numpy==1.26.4  scikit-learn==1.6.1  joblib==1.4.2
```

If the new model was trained with a different scikit-learn/Python, update
`requirements.txt` **and** record the real versions in
`model_metadata.json` (`training.sklearn_version`, `training.python_version`)
before deploying. Python 3.11 is the reference runtime.

## 6. How to verify successful loading

```bash
cd edu2job-flask-ai
python -m venv venv && source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
python app.py                                     # terminal 1

curl http://localhost:5000/health                 # terminal 2
```

Ready state looks like:

```json
{"status": "ok", "modelReady": true, "modelStatus": "READY",
 "modelVersion": "edu2job-v2", "labelsLoaded": 128, "missingArtifacts": []}
```

Until artifacts are installed you will instead get HTTP 503 with
`"modelReady": false` and a `detail` message naming the missing files.
That is the correct, honest behaviour — not an error to "fix" by copying
the old model in.

## 7. How to test a real prediction

```bash
curl -X POST http://localhost:5000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "degree": "B.Tech",
    "major": "Computer Science",
    "cgpa": 8.5,
    "skills": ["Python", "SQL", "Java"],
    "certification_name": null,
    "internship_experience": "Yes",
    "projects_count": 3,
    "communication_skill": "Advanced",
    "experience_months": 6
  }'
```

Expected (roles depend on the real model):

```json
{"modelVersion": "edu2job-v2",
 "predictions": [{"rank": 1, "role": "<role from your 128 labels>", "score": 0.0}]}
```

Then run the same request through Spring Boot (`POST /api/predict/` with a
JWT) and confirm the row lands in `pred_history` with
`model_version = 'edu2job-v2'` and all nine input columns populated.

Adapter behaviour (with fake estimators, not the real model) is covered by:

```bash
pip install -r requirements-dev.txt
pytest
```

## 8. Exactly what is still missing (as of this migration)

1. `ml_models/v2/model.pkl` — the fitted MultiOutput Random Forest (or Pipeline).
2. `ml_models/v2/job_role_labels.json` — the 128 role names in training output order (or equivalent encoder pickle).
3. `ml_models/v2/preprocessor.pkl` — the fitted 9 → 538 feature pipeline, if not already inside `model.pkl`.
4. `ml_models/v2/model_metadata.json` — decode mode + vocabularies + training versions (template in §3).
5. `edu2job_dataset.csv` (10,000 × 10) — only needed to confirm category
   vocabularies/finalize form options; not needed at inference time.

Items 1–4 are the blockers for genuine inference. Everything else
(React form, Spring API, database, history) already speaks the new schema.
