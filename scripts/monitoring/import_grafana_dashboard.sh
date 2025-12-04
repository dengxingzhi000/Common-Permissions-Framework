#!/usr/bin/env bash
set -euo pipefail

# Usage: GRAFANA_URL=http://localhost:3000 GRAFANA_TOKEN=... \
#        ./scripts/monitoring/import_grafana_dashboard.sh monitoring/grafana/dashboards/security-overview.json

GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"
GRAFANA_TOKEN="${GRAFANA_TOKEN:-}"
DASHBOARD_JSON_PATH="${1:-monitoring/grafana/dashboards/security-overview.json}"

if [[ -z "${GRAFANA_TOKEN}" ]]; then
  echo "GRAFANA_TOKEN not set" >&2
  exit 1
fi

echo "Importing dashboard ${DASHBOARD_JSON_PATH} into ${GRAFANA_URL} ..."

PAYLOAD=$(jq -n --argjson dashboard "$(cat "${DASHBOARD_JSON_PATH}")" '{dashboard: $dashboard, overwrite: true, folderId: 0}')

curl -sS -X POST \
  -H "Authorization: Bearer ${GRAFANA_TOKEN}" \
  -H 'Content-Type: application/json' \
  --data "${PAYLOAD}" \
  "${GRAFANA_URL}/api/dashboards/db" | jq .

echo "Done."

