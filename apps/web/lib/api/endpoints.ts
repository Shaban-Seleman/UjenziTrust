import { z } from "zod";
import { apiFetch } from "@/lib/api/client";
import {
  actorSchema,
  disbursementSchema,
  escrowSchema,
  inspectionSchema,
  ledgerEntrySchema,
  milestoneSchema,
  offerSchema,
  outboxEventSchema,
  pageSchema,
  projectSchema,
  propertySchema,
  reservationSchema,
  webhookEventSchema
} from "@/lib/api/schemas";

const pageOrArray = <T extends z.ZodTypeAny>(schema: T) => z.union([z.array(schema), pageSchema(schema)]);

function toItems<T>(data: T[] | { content: T[] }) {
  return Array.isArray(data) ? data : data.content;
}

export const authMe = () => apiFetch("/api/auth/me", { method: "GET" }, actorSchema);
export const login = (email: string, password: string) =>
  apiFetch("/api/auth/login", { method: "POST", body: JSON.stringify({ email, password }) });
export const logout = () => apiFetch("/api/auth/logout", { method: "POST" });

export async function listProperties() {
  const data = await apiFetch("market/properties", { method: "GET" }, pageOrArray(propertySchema));
  return toItems(data);
}

export const getProperty = (id: string) => apiFetch(`market/properties/${id}`, { method: "GET" }, propertySchema);
export const createProperty = (payload: Record<string, unknown>) =>
  apiFetch("market/properties", { method: "POST", body: JSON.stringify(payload) }, propertySchema);
export const publishProperty = (id: string) => apiFetch(`market/properties/${id}/publish`, { method: "POST" }, propertySchema);

export async function listOffers() {
  const data = await apiFetch("market/offers", { method: "GET" }, pageOrArray(offerSchema));
  return toItems(data);
}

export const submitOffer = (propertyId: string, payload: Record<string, unknown>) =>
  apiFetch(`market/properties/${propertyId}/offers`, { method: "POST", body: JSON.stringify(payload) }, offerSchema);

export const acceptOffer = (offerId: string, payload: { idempotencyKey: string; notes?: string }) =>
  apiFetch(`market/offers/${offerId}/accept`, { method: "POST", body: JSON.stringify(payload) });

export const rejectOffer = (offerId: string, notes?: string) =>
  apiFetch(`market/offers/${offerId}/reject`, { method: "POST", body: JSON.stringify({ notes }) }, offerSchema);

export const withdrawOffer = (offerId: string, notes?: string) =>
  apiFetch(`market/offers/${offerId}/withdraw`, { method: "POST", body: JSON.stringify({ notes }) }, offerSchema);

export async function listReservations() {
  const data = await apiFetch("market/reservations", { method: "GET" }, pageOrArray(reservationSchema));
  return toItems(data);
}

export async function listEscrows() {
  const data = await apiFetch("ops/escrows", { method: "GET" }, pageOrArray(escrowSchema));
  return toItems(data);
}

export const getEscrow = (id: string) => apiFetch(`ops/escrows/${id}`, { method: "GET" }, escrowSchema);

export async function listDisbursementsByEscrow(escrowId: string) {
  const data = await apiFetch(`ops/escrows/${escrowId}/disbursements`, { method: "GET" }, pageOrArray(disbursementSchema));
  return toItems(data);
}

export async function listProjects() {
  const data = await apiFetch("build/projects", { method: "GET" }, pageOrArray(projectSchema));
  return toItems(data);
}

export const getProject = (id: string) => apiFetch(`build/projects/${id}`, { method: "GET" }, projectSchema);
export const listMilestones = (projectId: string) => apiFetch(`build/projects/${projectId}/milestones`, { method: "GET" }, z.array(milestoneSchema));

export const submitMilestoneEvidence = (milestoneId: string, payload: Record<string, unknown>) =>
  apiFetch(`build/milestones/${milestoneId}/submit`, { method: "POST", body: JSON.stringify(payload) });

export const requestInspection = (payload: Record<string, unknown>) =>
  apiFetch("build/inspections/schedule", { method: "POST", body: JSON.stringify(payload) }, inspectionSchema);

export const completeInspection = (inspectionId: string, payload: Record<string, unknown>) =>
  apiFetch(`build/inspections/${inspectionId}/complete`, { method: "POST", body: JSON.stringify(payload) }, inspectionSchema);

export const approveMilestoneSingle = (milestoneId: string, idempotencyKey: string) =>
  apiFetch(`build/milestones/${milestoneId}/approve`, { method: "POST", body: JSON.stringify({ idempotencyKey }) }, milestoneSchema);

export const approveMilestoneMulti = (milestoneId: string, payload: Record<string, unknown>) =>
  apiFetch(`build/milestones/${milestoneId}/approve-multi`, { method: "POST", body: JSON.stringify(payload) }, milestoneSchema);

export const releaseRetention = (milestoneId: string, idempotencyKey: string) =>
  apiFetch(`build/milestones/${milestoneId}/retention-release`, { method: "POST", body: JSON.stringify({ idempotencyKey }) }, milestoneSchema);

export async function listOutboxEvents() {
  const data = await apiFetch("ops/outbox", { method: "GET" }, pageOrArray(outboxEventSchema));
  return toItems(data);
}

export async function listWebhookEvents() {
  const data = await apiFetch("ops/webhooks/events", { method: "GET" }, pageOrArray(webhookEventSchema));
  return toItems(data);
}

export async function listLedgerEntries() {
  const data = await apiFetch("ledger/journal-entries", { method: "GET" }, pageOrArray(ledgerEntrySchema));
  return toItems(data);
}
