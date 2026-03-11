import { z } from "zod";
import { apiFetch } from "@/lib/api/client";
import {
  actorSchema,
  directoryUserSchema,
  demoSeedSummarySchema,
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

type PagedResult<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

async function fetchPage<TSchema extends z.ZodTypeAny>(
  path: string,
  schema: TSchema,
  page = 0,
  size = 10
): Promise<PagedResult<z.infer<TSchema>>> {
  const query = new URLSearchParams({ page: String(page), size: String(size) }).toString();
  const separator = path.includes("?") ? "&" : "?";
  const data = await apiFetch(`${path}${separator}${query}`, { method: "GET" }, pageSchema(schema));
  return data;
}

export const authMe = () => apiFetch("/api/auth/me", { method: "GET" }, actorSchema);
export const listDirectoryUsers = (role: "CONTRACTOR" | "INSPECTOR") =>
  apiFetch(`users/directory?role=${role}`, { method: "GET" }, z.array(directoryUserSchema));
export const login = (email: string, password: string) =>
  apiFetch("/api/auth/login", { method: "POST", body: JSON.stringify({ email, password }) });
export const logout = () => apiFetch("/api/auth/logout", { method: "POST" });

export function resetAndSeedDemoScenario(scenario = "investor_v1") {
  const query = new URLSearchParams({ scenario }).toString();
  return apiFetch(`admin/demo/reset-and-seed?${query}`, { method: "POST" }, demoSeedSummarySchema);
}

export async function listProperties() {
  const data = await apiFetch("market/properties", { method: "GET" }, pageOrArray(propertySchema));
  return toItems(data);
}

export async function listMyProperties() {
  const data = await apiFetch("market/properties/mine", { method: "GET" }, pageOrArray(propertySchema));
  return toItems(data);
}

export async function listPropertiesPage(page = 0, size = 10): Promise<PagedResult<z.infer<typeof propertySchema>>> {
  return fetchPage("market/properties", propertySchema, page, size);
}

export async function listMyPropertiesPage(page = 0, size = 10): Promise<PagedResult<z.infer<typeof propertySchema>>> {
  return fetchPage("market/properties/mine", propertySchema, page, size);
}

export const getProperty = (id: string) => apiFetch(`market/properties/${id}`, { method: "GET" }, propertySchema);
export const createProperty = (payload: Record<string, unknown>) =>
  apiFetch("market/properties", { method: "POST", body: JSON.stringify(payload) }, propertySchema);
export const publishProperty = (id: string) => apiFetch(`market/properties/${id}/publish`, { method: "POST" }, propertySchema);

export async function listOffers() {
  const data = await apiFetch("market/offers", { method: "GET" }, pageOrArray(offerSchema));
  return toItems(data);
}

export async function listOffersPage(page = 0, size = 10): Promise<PagedResult<z.infer<typeof offerSchema>>> {
  return fetchPage("market/offers", offerSchema, page, size);
}

export const submitOffer = (propertyId: string, payload: Record<string, unknown>) =>
  apiFetch(`market/properties/${propertyId}/offers`, { method: "POST", body: JSON.stringify(payload) }, offerSchema);

export const acceptOffer = (offerId: string, payload: { idempotencyKey: string; notes?: string }) =>
  apiFetch(`market/offers/${offerId}/accept`, { method: "POST", body: JSON.stringify(payload) });

export const counterOffer = (offerId: string, payload: { amount: number; notes?: string }) =>
  apiFetch(`market/offers/${offerId}/counter`, { method: "POST", body: JSON.stringify(payload) }, offerSchema);

export const rejectOffer = (offerId: string, notes?: string) =>
  apiFetch(`market/offers/${offerId}/reject`, { method: "POST", body: JSON.stringify({ notes }) }, offerSchema);

export const withdrawOffer = (offerId: string, notes?: string) =>
  apiFetch(`market/offers/${offerId}/withdraw`, { method: "POST", body: JSON.stringify({ notes }) }, offerSchema);

export const cancelReservation = (reservationId: string, notes?: string) =>
  apiFetch(`market/reservations/${reservationId}/cancel`, { method: "POST", body: JSON.stringify({ notes }) }, reservationSchema);

export async function listReservations() {
  const data = await apiFetch("market/reservations", { method: "GET" }, pageOrArray(reservationSchema));
  return toItems(data);
}

export async function listReservationsPage(page = 0, size = 10): Promise<PagedResult<z.infer<typeof reservationSchema>>> {
  return fetchPage("market/reservations", reservationSchema, page, size);
}

export async function listEscrows() {
  const data = await apiFetch("ops/escrows", { method: "GET" }, pageOrArray(escrowSchema));
  return toItems(data);
}

export async function listEscrowsPage(page = 0, size = 10): Promise<PagedResult<z.infer<typeof escrowSchema>>> {
  return fetchPage("ops/escrows", escrowSchema, page, size);
}

export const getEscrow = (id: string) => apiFetch(`ops/escrows/${id}`, { method: "GET" }, escrowSchema);

export async function listDisbursementsByEscrow(escrowId: string) {
  const data = await apiFetch(`ops/escrows/${escrowId}/disbursements`, { method: "GET" }, pageOrArray(disbursementSchema));
  return toItems(data);
}

export async function listDisbursementsByEscrowPage(
  escrowId: string,
  page = 0,
  size = 10
): Promise<PagedResult<z.infer<typeof disbursementSchema>>> {
  return fetchPage(`ops/escrows/${escrowId}/disbursements`, disbursementSchema, page, size);
}

export async function listProjects() {
  const data = await apiFetch("build/projects", { method: "GET" }, pageOrArray(projectSchema));
  return toItems(data);
}

export async function listProjectsPage(page = 0, size = 10): Promise<PagedResult<z.infer<typeof projectSchema>>> {
  return fetchPage("build/projects", projectSchema, page, size);
}

export const createProject = (payload: Record<string, unknown>) =>
  apiFetch("build/projects", { method: "POST", body: JSON.stringify(payload) }, projectSchema);

export const assignProjectParticipants = (projectId: string, payload: { contractorUserId: string; inspectorUserId: string }) =>
  apiFetch(`build/projects/${projectId}/assign`, { method: "POST", body: JSON.stringify(payload) }, projectSchema);

export const activateProject = (projectId: string) =>
  apiFetch(`build/projects/${projectId}/activate`, { method: "POST" }, projectSchema);

export const getProject = (id: string) => apiFetch(`build/projects/${id}`, { method: "GET" }, projectSchema);

export const createMilestone = (projectId: string, payload: {
  name: string;
  description?: string;
  sequenceNo: number;
  amount: number;
  retentionAmount?: number;
  dueDate?: string;
}) =>
  apiFetch(`build/projects/${projectId}/milestones`, { method: "POST", body: JSON.stringify(payload) }, milestoneSchema);

export const getMilestone = (milestoneId: string) =>
  apiFetch(`build/milestones/${milestoneId}`, { method: "GET" }, milestoneSchema);

export async function listMilestones(projectId: string) {
  const data = await apiFetch(`build/projects/${projectId}/milestones`, { method: "GET" }, pageOrArray(milestoneSchema));
  return toItems(data);
}

export async function listMilestonesPage(projectId: string, page = 0, size = 10): Promise<PagedResult<z.infer<typeof milestoneSchema>>> {
  return fetchPage(`build/projects/${projectId}/milestones`, milestoneSchema, page, size);
}

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

export async function listOutboxEventsPage(page = 0, size = 10): Promise<PagedResult<z.infer<typeof outboxEventSchema>>> {
  return fetchPage("ops/outbox", outboxEventSchema, page, size);
}

export async function listWebhookEvents() {
  const data = await apiFetch("ops/webhooks/events", { method: "GET" }, pageOrArray(webhookEventSchema));
  return toItems(data);
}

export async function listWebhookEventsPage(page = 0, size = 10): Promise<PagedResult<z.infer<typeof webhookEventSchema>>> {
  return fetchPage("ops/webhooks/events", webhookEventSchema, page, size);
}

export async function listLedgerEntries() {
  const data = await apiFetch("ledger/journal-entries", { method: "GET" }, pageOrArray(ledgerEntrySchema));
  return toItems(data);
}

export async function listLedgerEntriesPage(page = 0, size = 10): Promise<PagedResult<z.infer<typeof ledgerEntrySchema>>> {
  return fetchPage("ledger/journal-entries", ledgerEntrySchema, page, size);
}
