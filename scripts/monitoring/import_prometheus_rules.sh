#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/monitoring/import_prometheus_rules.sh /path/to/prometheus.yml
# Requires: Prometheus with --web.enable-lifecycle enabled for reload.

RULE_SRC_DIR="monitoring/prometheus/rules"
PROMETHEUS_RULES_DIR="${PROMETHEUS_RULES_DIR:-/etc/prometheus/rules}"
PROMETHEUS_RELOAD_URL="${PROMETHEUS_RELOAD_URL:-http://localhost:9090/-/reload}"

echo "Copying rules to ${PROMETHEUS_RULES_DIR} ..."
mkdir -p "${PROMETHEUS_RULES_DIR}"
cp -f "${RULE_SRC_DIR}"/*.yaml "${PROMETHEUS_RULES_DIR}/" || true

echo "Triggering Prometheus reload ..."
curl -s -X POST "${PROMETHEUS_RELOAD_URL}" || echo "Warn: reload endpoint not reachable."

echo "Done."

