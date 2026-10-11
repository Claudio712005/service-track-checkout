#!/usr/bin/env bash
set -euo pipefail

AMBIENTE="${1:-}"
PROJETO="${PROJETO:-servicetrack}"
SERVICO="${SERVICO:-checkout}"
REGIAO="${AWS_REGION:-us-east-1}"
NAMESPACE="${NAMESPACE:-service-track-checkout}"
SEGREDO="service-track-checkout-mercado-pago"

if [[ "$AMBIENTE" != "hml" && "$AMBIENTE" != "prd" ]]; then
  echo "uso: $0 <hml|prd>" >&2
  exit 1
fi

ler() {
  local caminho="/$PROJETO/$AMBIENTE/$SERVICO/mercado-pago/$1"
  if ! aws ssm get-parameter --name "$caminho" --with-decryption --region "$REGIAO" \
      --query Parameter.Value --output text 2>/dev/null; then
    echo ""
  fi
}

TOKEN="$(ler access-token)"
SEGREDO_DO_WEBHOOK="$(ler webhook-secret)"

if [[ -z "$TOKEN" ]]; then
  echo "AVISO: /$PROJETO/$AMBIENTE/$SERVICO/mercado-pago/access-token nao existe no SSM."
  echo "AVISO: o segredo sai com token vazio. A cobranca responde erro do provedor, e o"
  echo "AVISO: webhook recusa toda notificacao enquanto o segredo de assinatura estiver vazio."
fi

kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f - >/dev/null

kubectl create secret generic "$SEGREDO" \
  --namespace "$NAMESPACE" \
  --from-literal=MERCADO_PAGO_ACCESS_TOKEN="$TOKEN" \
  --from-literal=MERCADO_PAGO_WEBHOOK_SECRET="$SEGREDO_DO_WEBHOOK" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "secret $SEGREDO aplicado em $NAMESPACE a partir do SSM de $AMBIENTE"
