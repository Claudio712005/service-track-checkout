#!/usr/bin/env bash
set -euo pipefail

AMBIENTE="${1:-}"
PROJETO="${PROJETO:-servicetrack}"
SERVICO="${SERVICO:-checkout}"
REGIAO="${AWS_REGION:-us-east-1}"
NAMESPACE="${NAMESPACE:-service-track-checkout}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ "$AMBIENTE" != "local" && "$AMBIENTE" != "hml" && "$AMBIENTE" != "prd" ]]; then
  echo "uso: $0 <local|hml|prd>" >&2
  exit 1
fi

if [[ "$AMBIENTE" == "local" ]]; then
  PG_HOST="service-track-checkout-postgres"
  PG_USER="st_chk_user"
  PG_SENHA="st_chk_local"
  PG_BANCO="st_chk"
else
  ler() {
    aws ssm get-parameter \
      --name "/$PROJETO/$AMBIENTE/$SERVICO/db/$1" \
      --with-decryption \
      --region "$REGIAO" \
      --query Parameter.Value \
      --output text
  }
  PG_HOST="$(ler endpoint)"
  PG_USER="$(ler username)"
  PG_SENHA="$(ler password)"
  PG_BANCO="$(ler name)"
fi

kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f - >/dev/null

aplicar_sql() {
  local arquivo="$1"
  echo "postgres <- $(basename "$arquivo")"
  kubectl run "baseline-pg-$RANDOM" \
    --namespace "$NAMESPACE" \
    --rm --stdin --quiet --restart=Never \
    --image=postgres:16 \
    --env "PGPASSWORD=$PG_SENHA" \
    --command -- psql -h "$PG_HOST" -U "$PG_USER" -d "$PG_BANCO" -v ON_ERROR_STOP=1 -f - \
    < "$arquivo"
}

for arquivo in "$RAIZ"/db/postgres/*.sql; do
  aplicar_sql "$arquivo"
done

echo "baseline de $AMBIENTE aplicado. Reexecutar e seguro: os scripts sao idempotentes."
