#!/usr/bin/env bash
set -Eeuo pipefail

for variable in BACKEND_IMAGE FRONTEND_IMAGE CORS_ALLOWED_ORIGINS GHCR_USERNAME GHCR_TOKEN; do
  if [[ -z "${!variable:-}" ]]; then
    printf 'Required deployment variable is missing: %s\n' "$variable" >&2
    exit 1
  fi
done

if [[ ! -f .env ]]; then
  echo "No existe .env en ~/TEO1_ClinicaDental" >&2
  exit 1
fi

for command_name in curl python3; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    printf 'Required health-check command is missing: %s\n' "$command_name" >&2
    exit 1
  fi
done

printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin

persist_env_var() {
  local key="$1"
  local value="$2"
  if grep -q "^${key}=" .env; then
    sed -i -E "s|^${key}=.*$|${key}=${value}|" .env
  else
    printf '%s\n' "${key}=${value}" >> .env
  fi
}

persist_env_var BACKEND_IMAGE "$BACKEND_IMAGE"
persist_env_var FRONTEND_IMAGE "$FRONTEND_IMAGE"
persist_env_var CORS_ALLOWED_ORIGINS "$CORS_ALLOWED_ORIGINS"

docker compose pull backend frontend
db_container_id="$(docker compose ps --all --quiet db)"
if [[ -z "$db_container_id" ]]; then
  docker compose up -d --no-deps db
  db_container_id="$(docker compose ps --all --quiet db)"
elif [[ -z "$(docker compose ps --quiet db)" ]]; then
  docker compose start db
fi
if [[ -z "$db_container_id" ]]; then
  echo "No se pudo resolver el contenedor db de Docker Compose" >&2
  exit 1
fi

docker update --restart unless-stopped "$db_container_id"
db_status=""
attempt=0
while [[ "$attempt" -lt 60 ]]; do
  db_status="$(docker inspect --format '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$db_container_id")"
  if [[ "$db_status" == "running healthy" ]]; then
    break
  fi
  attempt=$((attempt + 1))
  sleep 2
done
if [[ "$db_status" != "running healthy" ]]; then
  echo "El contenedor db no alcanzó el estado healthy" >&2
  exit 1
fi

docker compose up -d --no-build --no-deps --force-recreate backend frontend

response_file="$(mktemp)"
trap 'rm -f "$response_file"' EXIT
health_check_deadline=$((SECONDS + 180))

wait_for_http() {
  local label="$1"
  local url="$2"
  local expected_content="${3:-}"
  local output_file="/dev/null"
  local attempt=0

  if [[ -n "$expected_content" ]]; then
    output_file="$response_file"
  fi

  while [[ "$attempt" -lt 90 ]] && (( SECONDS < health_check_deadline )); do
    if curl --fail --silent --show-error --location --max-time 10 --output "$output_file" "$url" 2>/dev/null \
      && { [[ -z "$expected_content" ]] || grep --quiet --ignore-case --fixed-strings "$expected_content" "$response_file"; }; then
      printf '%s: disponible\n' "$label"
      return 0
    fi
    attempt=$((attempt + 1))
    sleep 2
  done

  printf '%s no respondió correctamente dentro de la ventana máxima de arranque de 180 segundos: %s\n' "$label" "$url" >&2
  return 1
}

wait_for_openapi() {
  local label="$1"
  local url="$2"
  local attempt=0

  while [[ "$attempt" -lt 90 ]] && (( SECONDS < health_check_deadline )); do
    if curl --fail --silent --show-error --location --max-time 10 --output "$response_file" "$url" 2>/dev/null \
      && python3 - "$response_file" <<'PY'
import json
import sys

try:
    with open(sys.argv[1], encoding="utf-8") as response:
        document = json.load(response)
except (OSError, json.JSONDecodeError):
    sys.exit(1)

info = document.get("info", {})
if (
    not isinstance(document.get("openapi"), str)
    or not document["openapi"].startswith("3.")
    or not isinstance(info, dict)
    or info.get("title") != "Clínica Dental API"
    or info.get("version") != "1.0.0"
    or not isinstance(document.get("paths"), dict)
    or not document["paths"]
):
    sys.exit(1)
PY
    then
      printf '%s: documento OpenAPI válido\n' "$label"
      return 0
    fi
    attempt=$((attempt + 1))
    sleep 2
  done

  printf '%s no entregó un documento OpenAPI válido dentro de la ventana máxima de arranque de 180 segundos: %s\n' "$label" "$url" >&2
  return 1
}

wait_for_openapi "Backend /v3/api-docs" "http://127.0.0.1:8080/v3/api-docs"
wait_for_http "Frontend servido por nginx" "http://127.0.0.1:4200/" "<app-root"
wait_for_http "Swagger UI mediante nginx" "http://127.0.0.1:4200/api/swagger-ui.html" "swagger-ui"
wait_for_openapi "OpenAPI mediante nginx /api/v3/api-docs" "http://127.0.0.1:4200/api/v3/api-docs"

docker image prune -f
