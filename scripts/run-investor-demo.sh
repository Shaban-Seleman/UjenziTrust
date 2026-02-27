#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
WEBHOOK_SECRET="${WEBHOOK_SECRET:-change-me-webhook-secret}"

require_bin() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required binary: $1" >&2
    exit 1
  fi
}

require_bin curl
require_bin jq
require_bin openssl

call_api() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local body="${4:-}"
  local extra_header="${5:-}"

  local tmp_body tmp_status
  tmp_body="$(mktemp)"
  tmp_status="$(mktemp)"

  local -a cmd
  cmd=(curl -sS -X "$method" -o "$tmp_body" -w "%{http_code}" "$BASE_URL$path" -H "Content-Type: application/json")
  if [[ -n "$token" ]]; then
    cmd+=(-H "Authorization: Bearer $token")
  fi
  if [[ -n "$extra_header" ]]; then
    cmd+=(-H "$extra_header")
  fi
  if [[ -n "$body" ]]; then
    cmd+=(-d "$body")
  fi

  "${cmd[@]}" >"$tmp_status"
  local status
  status="$(cat "$tmp_status")"
  rm -f "$tmp_status"

  if [[ ! "$status" =~ ^2 ]]; then
    echo "Request failed: $method $path -> HTTP $status" >&2
    cat "$tmp_body" >&2
    rm -f "$tmp_body"
    exit 1
  fi

  cat "$tmp_body"
  rm -f "$tmp_body"
}

login() {
  local identifier="$1"
  call_api POST "/auth/login" "" "{\"identifier\":\"$identifier\"}" | jq -r '.accessToken'
}

echo "[Investor Demo] Logging in seeded actors..."
OWNER_TOKEN="$(login "investor.owner@ujenzi.demo")"
BUYER_TOKEN="$(login "diaspora.buyer@ujenzi.demo")"
CONTRACTOR_TOKEN="$(login "contractor.site@ujenzi.demo")"
INSPECTOR_TOKEN="$(login "inspector.qa@ujenzi.demo")"
ADMIN_TOKEN="$(login "ops.admin@ujenzi.demo")"

OWNER_ID="$(call_api GET "/auth/me" "$OWNER_TOKEN" | jq -r '.userId')"
CONTRACTOR_ID="$(call_api GET "/auth/me" "$CONTRACTOR_TOKEN" | jq -r '.userId')"
INSPECTOR_ID="$(call_api GET "/auth/me" "$INSPECTOR_TOKEN" | jq -r '.userId')"

RUN_KEY="$(date -u +%Y%m%d%H%M%S)"
PROPERTY_TITLE="Investor Demo Property ${RUN_KEY}"
PROJECT_TITLE="Investor Demo Project ${RUN_KEY}"

echo "[Investor Demo] Step 1/9: Create and publish property"
PROPERTY_JSON="$(call_api POST "/market/properties" "$OWNER_TOKEN" "$(jq -nc \
  --arg title "$PROPERTY_TITLE" \
  '{title:$title,description:"Ocean-view asset for diaspora portfolio",location:"Dar es Salaam",askingPrice:750000000,currency:"TZS"}')")"
PROPERTY_ID="$(echo "$PROPERTY_JSON" | jq -r '.id')"
call_api POST "/market/properties/${PROPERTY_ID}/publish" "$OWNER_TOKEN" >/dev/null

echo "[Investor Demo] Step 2/9: Buyer submits and seller accepts offer"
OFFER_JSON="$(call_api POST "/market/properties/${PROPERTY_ID}/offers" "$BUYER_TOKEN" "$(jq -nc \
  '{amount:740000000,currency:"TZS",notes:"Cash-ready diaspora investor"}')")"
OFFER_ID="$(echo "$OFFER_JSON" | jq -r '.id')"
ACCEPT_JSON="$(call_api POST "/market/offers/${OFFER_ID}/accept" "$OWNER_TOKEN" "$(jq -nc \
  --arg k "INVESTOR-DEMO-OFFER-${RUN_KEY}" \
  '{idempotencyKey:$k,notes:"Accepted for investor demo"}')")"
RESERVATION_ID="$(echo "$ACCEPT_JSON" | jq -r '.reservation.id')"

ESCROW_JSON="$(call_api GET "/ops/escrows?page=0&size=50" "$BUYER_TOKEN")"
PROPERTY_ESCROW_ID="$(echo "$ESCROW_JSON" | jq -r --arg bk "PROPERTY_PURCHASE:${RESERVATION_ID}" '.content[] | select(.businessKey == $bk) | .id')"

echo "[Investor Demo] Step 3/9: Owner creates and activates project"
PROJECT_JSON="$(call_api POST "/build/projects" "$OWNER_TOKEN" "$(jq -nc \
  --arg title "$PROJECT_TITLE" \
  '{title:$title,description:"Villa construction for investor",totalBudget:120000000,currency:"TZS",retentionRate:10}')")"
PROJECT_ID="$(echo "$PROJECT_JSON" | jq -r '.id')"
PROJECT_ESCROW_ID="$(echo "$PROJECT_JSON" | jq -r '.escrowId')"

call_api POST "/build/projects/${PROJECT_ID}/assign" "$OWNER_TOKEN" "$(jq -nc \
  --arg c "$CONTRACTOR_ID" \
  --arg i "$INSPECTOR_ID" \
  '{contractorUserId:$c,inspectorUserId:$i}')" >/dev/null
call_api POST "/build/projects/${PROJECT_ID}/activate" "$OWNER_TOKEN" >/dev/null

echo "[Investor Demo] Step 4/9: Create milestone and submit evidence"
MILESTONE_JSON="$(call_api POST "/build/projects/${PROJECT_ID}/milestones" "$OWNER_TOKEN" "$(jq -nc \
  '{name:"Foundation Complete",description:"Foundation and slab done",sequenceNo:1,amount:30000000,retentionAmount:3000000,dueDate:"2026-03-31"}')")"
MILESTONE_ID="$(echo "$MILESTONE_JSON" | jq -r '.id')"
call_api POST "/build/milestones/${MILESTONE_ID}/submit" "$CONTRACTOR_TOKEN" "$(jq -nc \
  '{evidence:{photos:["https://demo.local/foundation-1.jpg"],certificate:"engineer-signoff"},notes:"Submitted for approval"}')" >/dev/null

echo "[Investor Demo] Step 5/9: Approve milestone and create payout disbursement"
call_api POST "/build/milestones/${MILESTONE_ID}/approve" "$OWNER_TOKEN" "$(jq -nc \
  --arg k "INVESTOR-DEMO-MILESTONE-${RUN_KEY}" \
  '{idempotencyKey:$k}')" >/dev/null

DISB_PAGE="$(call_api GET "/ops/escrows/${PROJECT_ESCROW_ID}/disbursements?page=0&size=20" "$OWNER_TOKEN")"
DISBURSEMENT_ID="$(echo "$DISB_PAGE" | jq -r --arg m "$MILESTONE_ID" '.content[] | select(.milestoneId == $m) | .id' | head -n1)"

if [[ -z "$DISBURSEMENT_ID" || "$DISBURSEMENT_ID" == "null" ]]; then
  echo "No project disbursement found for milestone ${MILESTONE_ID}" >&2
  exit 1
fi

echo "[Investor Demo] Step 6/9: Simulate bank settlement webhook"
EVENT_TS="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
EVENT_ID="INVESTOR-DEMO-SETTLEMENT-${RUN_KEY}"
SETTLEMENT_REF="SETTLEMENT-${RUN_KEY}"
WEBHOOK_BODY="$(jq -nc \
  --arg eventId "$EVENT_ID" \
  --arg eventTs "$EVENT_TS" \
  --arg disb "$DISBURSEMENT_ID" \
  --arg ref "$SETTLEMENT_REF" \
  '{eventId:$eventId,eventType:"DISBURSEMENT_SETTLED",eventTs:$eventTs,payload:{disbursementId:$disb,settlementRef:$ref}}')"
SIGNING_PAYLOAD="${EVENT_TS}.${WEBHOOK_BODY}"
SIGNATURE="$(printf "%s" "$SIGNING_PAYLOAD" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" | awk '{print $NF}')"
call_api POST "/ops/webhooks/settlement" "" "$WEBHOOK_BODY" "X-Signature: ${SIGNATURE}" >/dev/null

echo "[Investor Demo] Step 7/9: Verify milestone paid state"
MILESTONE_AFTER="$(call_api GET "/build/projects/${PROJECT_ID}/milestones" "$OWNER_TOKEN" | jq -c --arg id "$MILESTONE_ID" '.[] | select(.id == $id)')"
MILESTONE_STATUS="$(echo "$MILESTONE_AFTER" | jq -r '.status')"
RETENTION_RELEASE_AT="$(echo "$MILESTONE_AFTER" | jq -r '.retentionReleaseAt')"

echo "[Investor Demo] Step 8/9: Verify monitoring tables"
OUTBOX_COUNT="$(call_api GET "/ops/outbox?page=0&size=50" "$ADMIN_TOKEN" | jq -r '.totalElements')"
WEBHOOK_COUNT="$(call_api GET "/ops/webhooks/events?page=0&size=50" "$ADMIN_TOKEN" | jq -r '.totalElements')"
LEDGER_COUNT="$(call_api GET "/ledger/journal-entries?page=0&size=100" "$ADMIN_TOKEN" | jq -r '.totalElements')"

echo "[Investor Demo] Step 9/9: Summary"
echo "propertyId=${PROPERTY_ID}"
echo "offerId=${OFFER_ID}"
echo "reservationId=${RESERVATION_ID}"
echo "propertyPurchaseEscrowId=${PROPERTY_ESCROW_ID}"
echo "projectId=${PROJECT_ID}"
echo "projectEscrowId=${PROJECT_ESCROW_ID}"
echo "milestoneId=${MILESTONE_ID}"
echo "milestoneStatus=${MILESTONE_STATUS}"
echo "retentionReleaseAt=${RETENTION_RELEASE_AT}"
echo "settledDisbursementId=${DISBURSEMENT_ID}"
echo "settlementRef=${SETTLEMENT_REF}"
echo "outboxEvents(total)=${OUTBOX_COUNT}"
echo "webhookEvents(total)=${WEBHOOK_COUNT}"
echo "ledgerEntries(total)=${LEDGER_COUNT}"

if [[ "$MILESTONE_STATUS" != "PAID" ]]; then
  echo "Expected milestone to be PAID after settlement, got ${MILESTONE_STATUS}" >&2
  exit 1
fi

echo "[Investor Demo] PASS"
