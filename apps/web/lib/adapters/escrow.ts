import type { Escrow } from "@/lib/api/schemas";

export type EscrowTimelineData = {
  status: string;
  escrowId?: string;
  timestamps: {
    initiatedAt?: string;
    fundedAt?: string;
    activeAt?: string;
    completedAt?: string;
    cancelledAt?: string;
    disputedAt?: string;
  };
};

function asRecord(value: unknown) {
  return value && typeof value === "object" ? (value as Record<string, unknown>) : {};
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

export function adaptEscrowTimeline(escrow?: Escrow | null): EscrowTimelineData {
  const raw = asRecord(escrow);

  return {
    status: escrow?.status ?? "INITIATED",
    escrowId: escrow?.id,
    timestamps: {
      initiatedAt: getString(raw, ["initiatedAt", "createdAt"]),
      fundedAt: getString(raw, ["fundedAt"]),
      activeAt: getString(raw, ["activeAt"]),
      completedAt: getString(raw, ["completedAt", "releasedAt"]),
      cancelledAt: getString(raw, ["cancelledAt"]),
      disputedAt: getString(raw, ["disputedAt"])
    }
  };
}
