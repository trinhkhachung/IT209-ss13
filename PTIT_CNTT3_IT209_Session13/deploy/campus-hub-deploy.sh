#!/usr/bin/env bash
set -euo pipefail

if [[ "${SSH_ORIGINAL_COMMAND:-}" =~ ^deploy\ ([0-9a-f]{40})$ ]]; then
  image_sha="${BASH_REMATCH[1]}"
else
  printf '%s\n' 'Rejected deployment command.' >&2
  exit 64
fi

app_image="ghcr.io/pedguedes090/campus-hub-events:sha-${image_sha}"
compose_file="/opt/campus-hub-events/compose.production.yaml"
env_file="/opt/campus-hub-events/.env"
export APP_IMAGE="$app_image"

docker pull "$app_image"
docker compose --env-file "$env_file" -f "$compose_file" up -d --no-build --remove-orphans app
docker compose --env-file "$env_file" -f "$compose_file" ps
