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
docker image prune -f
