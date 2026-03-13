import { z } from "zod";

const maybeDate = z.string().datetime().or(z.string()).nullable().optional();

export const actorSchema = z.object({
  userId: z.string().uuid(),
  roles: z.array(z.string()),
  email: z.string().email().optional(),
  name: z.string().optional()
}).passthrough();

export const directoryUserSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email().nullable().optional(),
  phone: z.string().nullable().optional(),
  role: z.string()
}).passthrough();

export const propertySchema = z.object({
  id: z.string().uuid(),
  ownerUserId: z.string().uuid().optional(),
  title: z.string(),
  description: z.string().nullable().optional(),
  location: z.string().nullable().optional(),
  askingPrice: z.coerce.number().nullable().optional(),
  currency: z.string().optional().default("TZS"),
  status: z.string(),
  publishedAt: maybeDate
}).passthrough();

export const offerSchema = z.object({
  id: z.string().uuid(),
  propertyId: z.string().uuid().nullable().optional(),
  buyerUserId: z.string().uuid().optional(),
  sellerUserId: z.string().uuid().optional(),
  amount: z.coerce.number(),
  currency: z.string().optional().default("TZS"),
  status: z.string(),
  expiresAt: maybeDate
}).passthrough();

export const reservationSchema = z.object({
  id: z.string().uuid(),
  propertyId: z.string().uuid().nullable().optional(),
  offerId: z.string().uuid().nullable().optional(),
  buyerUserId: z.string().uuid().optional(),
  sellerUserId: z.string().uuid().optional(),
  status: z.string(),
  reservedUntil: maybeDate,
  escrowId: z.string().uuid().nullable().optional()
}).passthrough();

export const escrowSchema = z.object({
  id: z.string().uuid(),
  businessKey: z.string(),
  escrowType: z.string().optional(),
  status: z.string(),
  currency: z.string().optional().default("TZS"),
  totalAmount: z.coerce.number().optional(),
  fundedAmount: z.coerce.number().optional(),
  payerUserId: z.string().uuid().nullable().optional(),
  beneficiaryUserId: z.string().uuid().nullable().optional()
}).passthrough();

export const disbursementSchema = z.object({
  id: z.string().uuid(),
  businessKey: z.string(),
  escrowId: z.string().uuid().nullable().optional(),
  milestoneId: z.string().uuid().nullable().optional(),
  projectId: z.string().uuid().nullable().optional(),
  payeeType: z.string(),
  payeeId: z.string().uuid().or(z.string()),
  amount: z.coerce.number(),
  currency: z.string().optional().default("TZS"),
  status: z.string(),
  settlementRef: z.string().nullable().optional(),
  instructionRef: z.string().nullable().optional(),
  bankReference: z.string().nullable().optional(),
  createdAt: maybeDate,
  updatedAt: maybeDate
}).passthrough();

export const projectSchema = z.object({
  id: z.string().uuid(),
  ownerUserId: z.string().uuid(),
  contractorUserId: z.string().uuid().nullable().optional(),
  inspectorUserId: z.string().uuid().nullable().optional(),
  escrowId: z.string().uuid().nullable().optional(),
  propertyId: z.string().uuid().nullable().optional(),
  title: z.string(),
  description: z.string().nullable().optional(),
  status: z.string(),
  retentionRate: z.coerce.number().optional()
}).passthrough();

export const milestoneSchema = z.object({
  id: z.string().uuid(),
  projectId: z.string().uuid().nullable().optional(),
  name: z.string(),
  description: z.string().nullable().optional(),
  sequenceNo: z.number(),
  amount: z.coerce.number(),
  retentionAmount: z.coerce.number().optional(),
  status: z.string(),
  dueDate: maybeDate,
  approvedAt: maybeDate,
  paidAt: maybeDate,
  retentionReleaseAt: maybeDate,
  inspectionStatus: z.string().nullable().optional(),
  inspectionResult: z.string().nullable().optional(),
  inspectionCompletedAt: maybeDate,
  inspectionId: z.string().uuid().nullable().optional()
}).passthrough();

export const inspectionSchema = z.object({
  id: z.string().uuid().optional(),
  inspectionId: z.string().uuid().optional(),
  projectId: z.string().uuid().nullable().optional(),
  milestoneId: z.string().uuid().nullable().optional(),
  inspectorUserId: z.string().uuid().optional(),
  inspectorId: z.string().uuid().optional(),
  status: z.string(),
  result: z.string().optional(),
  scheduledAt: maybeDate,
  completedAt: maybeDate,
  createdAt: maybeDate,
  feeAmount: z.coerce.number().nullable().optional(),
  reportJson: z.unknown().optional()
}).passthrough();

export const outboxEventSchema = z.object({
  id: z.string().uuid(),
  aggregateType: z.string().nullable().optional(),
  aggregateId: z.string().nullable().optional(),
  eventType: z.string(),
  payloadSummary: z.string().nullable().optional(),
  status: z.string(),
  idempotencyKey: z.string(),
  attempts: z.coerce.number().optional(),
  nextAttemptAt: maybeDate,
  lastError: z.string().nullable().optional(),
  createdAt: maybeDate,
  updatedAt: maybeDate
}).passthrough();

export const webhookEventSchema = z.object({
  id: z.string().uuid(),
  eventId: z.string(),
  provider: z.string().nullable().optional(),
  eventType: z.string().optional(),
  status: z.string(),
  relatedDisbursementId: z.string().uuid().nullable().optional(),
  relatedEscrowId: z.string().uuid().nullable().optional(),
  relatedMilestoneId: z.string().uuid().nullable().optional(),
  relatedSettlementRef: z.string().nullable().optional(),
  receivedAt: maybeDate,
  processedAt: maybeDate
}).passthrough();

export const ledgerEntrySchema = z.object({
  id: z.string().uuid(),
  entryType: z.string(),
  referenceType: z.string().nullable().optional(),
  referenceId: z.string().optional(),
  idempotencyKey: z.string().optional(),
  hash: z.string().optional(),
  prevHash: z.string().nullable().optional(),
  createdAt: maybeDate
}).passthrough();

export const outboxEventDetailSchema = outboxEventSchema.extend({
  payload: z.unknown().optional()
}).passthrough();

export const webhookEventDetailSchema = webhookEventSchema.extend({
  eventTs: maybeDate,
  payload: z.unknown().optional()
}).passthrough();

export const ledgerLineSchema = z.object({
  id: z.string().uuid(),
  accountCode: z.string(),
  accountName: z.string(),
  direction: z.string(),
  amount: z.coerce.number(),
  currency: z.string()
}).passthrough();

export const ledgerEntryDetailSchema = ledgerEntrySchema.extend({
  description: z.string().nullable().optional(),
  actorUserId: z.string().uuid().nullable().optional(),
  chainIndex: z.coerce.number().nullable().optional(),
  lines: z.array(ledgerLineSchema).default([])
}).passthrough();

export const jobStatusSchema = z.object({
  name: z.string(),
  enabled: z.boolean(),
  schedule: z.string(),
  lastStartedAt: maybeDate,
  lastCompletedAt: maybeDate,
  lastSuccessAt: maybeDate,
  lastError: z.string().nullable().optional(),
  lastMessage: z.string().nullable().optional()
}).passthrough();

export const systemHealthSchema = z.object({
  overallStatus: z.string(),
  databaseStatus: z.string(),
  schedulingEnabled: z.boolean(),
  docsEnabled: z.boolean(),
  queue: z.object({
    pendingOutboxEvents: z.coerce.number(),
    failedOutboxEvents: z.coerce.number()
  }),
  jobs: z.record(jobStatusSchema)
}).passthrough();

export const operatorAuditSchema = z.object({
  id: z.string().uuid(),
  actorUserId: z.string().uuid().nullable().optional(),
  actorRoles: z.unknown().optional(),
  actionType: z.string(),
  resourceType: z.string(),
  resourceId: z.string().nullable().optional(),
  outcome: z.string(),
  createdAt: maybeDate
}).passthrough();

export const operatorAuditDetailSchema = operatorAuditSchema.extend({
  reason: z.string().nullable().optional(),
  correlationId: z.string().nullable().optional(),
  requestPath: z.string().nullable().optional(),
  requestMethod: z.string().nullable().optional(),
  metadata: z.unknown().optional(),
  errorDetail: z.string().nullable().optional()
}).passthrough();

export const demoSeedSummarySchema = z.object({
  usersCreated: z.coerce.number(),
  propertiesCreated: z.coerce.number(),
  offersCreated: z.coerce.number(),
  escrowsCreated: z.coerce.number(),
  projectsCreated: z.coerce.number(),
  milestonesCreated: z.coerce.number(),
  disbursementsCreated: z.coerce.number(),
  settledDisbursements: z.coerce.number(),
  retentionReadyCount: z.coerce.number()
}).passthrough();

export const splitSchema = z.object({
  payeeType: z.string(),
  payeeId: z.string().uuid(),
  amount: z.coerce.number(),
  businessKey: z.string().optional()
});

export const pageSchema = <T extends z.ZodTypeAny>(item: T) =>
  z.object({
    content: z.array(item),
    totalElements: z.number(),
    totalPages: z.number(),
    number: z.number(),
    size: z.number()
  }).passthrough();

export type Actor = z.infer<typeof actorSchema>;
export type DirectoryUser = z.infer<typeof directoryUserSchema>;
export type Property = z.infer<typeof propertySchema>;
export type Offer = z.infer<typeof offerSchema>;
export type Reservation = z.infer<typeof reservationSchema>;
export type Escrow = z.infer<typeof escrowSchema>;
export type Disbursement = z.infer<typeof disbursementSchema>;
export type Project = z.infer<typeof projectSchema>;
export type Milestone = z.infer<typeof milestoneSchema>;
export type Inspection = z.infer<typeof inspectionSchema>;
export type DemoSeedSummary = z.infer<typeof demoSeedSummarySchema>;
export type SplitInput = z.infer<typeof splitSchema>;
export type OutboxEvent = z.infer<typeof outboxEventSchema>;
export type WebhookEvent = z.infer<typeof webhookEventSchema>;
export type LedgerEntry = z.infer<typeof ledgerEntrySchema>;
export type OutboxEventDetail = z.infer<typeof outboxEventDetailSchema>;
export type WebhookEventDetail = z.infer<typeof webhookEventDetailSchema>;
export type LedgerEntryDetail = z.infer<typeof ledgerEntryDetailSchema>;
export type SystemHealth = z.infer<typeof systemHealthSchema>;
export type OperatorAudit = z.infer<typeof operatorAuditSchema>;
export type OperatorAuditDetail = z.infer<typeof operatorAuditDetailSchema>;
