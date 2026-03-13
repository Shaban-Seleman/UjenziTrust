import type { Milestone } from "@/lib/api/schemas";

export type AdaptedMilestone = {
  id: string;
  sequence: number;
  name: string;
  status: string;
  grossAmount?: number;
  retentionAmount?: number;
  paidAt?: string;
  retentionReleaseAt?: string;
  totalDisbursements?: number;
  settledDisbursements?: number;
  hasFailures?: boolean;
  inspectionStatus?: string;
  inspectionResult?: string;
  inspectionCompletedAt?: string;
  inspectionId?: string;
};

function asRecord(value: unknown) {
  return value && typeof value === "object" ? (value as Record<string, unknown>) : {};
}

function getNumber(record: Record<string, unknown>, keys: string[]) {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === "number" && Number.isFinite(value)) {
      return value;
    }
    if (typeof value === "string" && value.length > 0) {
      const parsed = Number(value);
      if (Number.isFinite(parsed)) {
        return parsed;
      }
    }
  }
  return undefined;
}

function getString(record: Record<string, unknown>, keys: string[]) {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === "string" && value.length > 0) {
      return value;
    }
  }
  return undefined;
}

function readDisbursementSummary(record: Record<string, unknown>) {
  const totalDisbursements = getNumber(record, ["totalDisbursements", "disbursementCount"]);
  const settledDisbursements = getNumber(record, ["settledDisbursements", "settledCount"]);

  if (totalDisbursements !== undefined || settledDisbursements !== undefined) {
    return {
      totalDisbursements,
      settledDisbursements,
      hasFailures: Boolean(record.hasFailures ?? (getNumber(record, ["failedDisbursements", "failureCount"]) ?? 0) > 0)
    };
  }

  const disbursements = Array.isArray(record.disbursements) ? record.disbursements : null;
  if (!disbursements) {
    return {};
  }

  const total = disbursements.length;
  const settled = disbursements.filter((item) => asRecord(item).status === "SETTLED").length;
  const hasFailures = disbursements.some((item) => asRecord(item).status === "FAILED");

  return {
    totalDisbursements: total,
    settledDisbursements: settled,
    hasFailures
  };
}

export function adaptMilestone(milestone: Milestone, index: number): AdaptedMilestone {
  const raw = asRecord(milestone);
  const summary = readDisbursementSummary(raw);

  return {
    id: milestone.id,
    sequence: getNumber(raw, ["sequenceNo", "seq", "order"]) ?? index + 1,
    name: getString(raw, ["name", "title"]) ?? `Milestone ${index + 1}`,
    status: getString(raw, ["status"]) ?? "PLANNED",
    grossAmount: getNumber(raw, ["amount", "grossAmount"]),
    retentionAmount: getNumber(raw, ["retentionAmount"]),
    paidAt: getString(raw, ["paidAt"]),
    retentionReleaseAt: getString(raw, ["retentionReleaseAt"]),
    totalDisbursements: summary.totalDisbursements,
    settledDisbursements: summary.settledDisbursements,
    hasFailures: summary.hasFailures,
    inspectionStatus: getString(raw, ["inspectionStatus"]),
    inspectionResult: getString(raw, ["inspectionResult"]),
    inspectionCompletedAt: getString(raw, ["inspectionCompletedAt", "completedAt"]),
    inspectionId: getString(raw, ["inspectionId"])
  };
}

export function adaptMilestones(milestones: Milestone[] = []) {
  return milestones.map((milestone, index) => adaptMilestone(milestone, index));
}
