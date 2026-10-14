"""EDU2JOB 2.0 Flask ML service.

Three-tier architecture: React -> Spring Boot -> this service.
Only Spring Boot calls these endpoints; browsers never talk to Flask.

Endpoints:
  GET  /health   -> service + model readiness (never just "process up")
  POST /predict  -> nine-field schema in, up to five ranked roles out

Error contract (consumed and translated by Spring Boot):
  400 {"error": {"code": "VALIDATION_ERROR", "message": ..., "fields": {...}}}
  503 {"error": {"code": "MODEL_NOT_READY", "message": ...}}
  502 {"error": {"code": "MODEL_OUTPUT_INVALID", "message": ...}}
  500 {"error": {"code": "INTERNAL_ERROR", "message": ...}}  (no internals)
"""
import config
from flask import Flask, jsonify, request

from input_schema import validate_payload
from prediction_service import (
    InvalidModelOutputError,
    ModelNotReadyError,
    get_bundle,
)


def _error(status, code, message, fields=None):
    body = {"error": {"code": code, "message": message}}
    if fields:
        body["error"]["fields"] = fields
    return jsonify(body), status


def create_app(bundle=None):
    app = Flask(__name__)
    model_bundle = bundle if bundle is not None else get_bundle()

    @app.get("/health")
    def health():
        info = model_bundle.health()
        # 200 when ready; 503 when the process runs but inference cannot.
        return jsonify(info), (200 if info["modelReady"] else 503)

    @app.post("/predict")
    def predict():
        if not model_bundle.ready:
            return _error(
                503, "MODEL_NOT_READY",
                "The new EDU2JOB 2.0 model is not ready. "
                + (model_bundle.detail or "Model artifacts are unavailable."),
            )

        data = request.get_json(silent=True)
        if data is None:
            return _error(400, "VALIDATION_ERROR", "Request body must be valid JSON.")

        cleaned, errors = validate_payload(data, model_bundle.vocabularies())
        if errors:
            return _error(400, "VALIDATION_ERROR",
                          "Invalid prediction input.", fields=errors)

        try:
            predictions = model_bundle.predict_top_roles(cleaned)
        except ModelNotReadyError as exc:
            return _error(503, "MODEL_NOT_READY", str(exc))
        except InvalidModelOutputError as exc:
            return _error(502, "MODEL_OUTPUT_INVALID", str(exc))
        except Exception:  # deliberately no stack trace / internals to clients
            app.logger.exception("Prediction failed")
            return _error(500, "INTERNAL_ERROR",
                          "Prediction failed unexpectedly. Please try again.")

        return jsonify({
            "modelVersion": model_bundle.model_version,
            "predictions": predictions,
        })

    return app


app = create_app()

if __name__ == "__main__":
    app.run(host=config.HOST, port=config.PORT)
