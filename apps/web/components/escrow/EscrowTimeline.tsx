"use client";

import { AlertTriangle, CheckCircle2, Circle, XCircle } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

type EscrowTimelineProps = {
  status: string;
  timestamps: {
    initiatedAt?: string;
    fundedAt?: string;
    activeAt?: string;
    completedAt?: string;
    cancelledAt?: string;
    disputedAt?: string;
  };
  escrowId?: string;
};

type StepKey = "INITIATED" | "FUNDED" | "ACTIVE" | "COMPLETED";

const baseSteps: Array<{ key: StepKey; label: string; timestampKey: keyof EscrowTimelineProps["timestamps"] }> = [
  { key: "INITIATED", label: "Initiated", timestampKey: "initiatedAt" },
  { key: "FUNDED", label: "Funded", timestampKey: "fundedAt" },
  { key: "ACTIVE", label: "Active", timestampKey: "activeAt" },
  { key: "COMPLETED", label: "Completed", timestampKey: "completedAt" }
];

const progression: Record<StepKey, number> = {
  INITIATED: 0,
  FUNDED: 1,
  ACTIVE: 2,
  COMPLETED: 3
};

function formatTimestamp(value?: string) {
  if (!value) {
    return "\u2014";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "\u2014";
  }

  return new Intl.DateTimeFormat("en-TZ", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(parsed);
}

function statusBadgeClass(status: string) {
  if (status === "COMPLETED") {
    return "bg-emerald-100 text-emerald-800";
  }
  if (status === "CANCELLED") {
    return "bg-red-100 text-red-700";
  }
  if (status === "DISPUTED") {
    return "bg-amber-100 text-amber-800";
  }
  return "bg-blue-100 text-blue-800";
}

export function EscrowTimeline({ status, timestamps, escrowId }: EscrowTimelineProps) {
  const currentIndex = status in progression ? progression[status as StepKey] : 0;
  const terminalTimestamp = status === "CANCELLED" ? timestamps.cancelledAt : status === "DISPUTED" ? timestamps.disputedAt : undefined;

  return (
    <Card aria-label={`Escrow timeline${escrowId ? ` for ${escrowId}` : ""}`}>
      <CardHeader className="gap-3 md:flex md:flex-row md:items-start md:justify-between">
        <div>
          <CardTitle>Escrow Timeline</CardTitle>
          <p className="text-sm text-muted-foreground">Track lifecycle stages and terminal states without assuming missing timestamps.</p>
        </div>
        <Badge className={statusBadgeClass(status)}>{status}</Badge>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between" role="list" aria-label="Escrow lifecycle steps">
          {baseSteps.map((step, index) => {
            const isReached = index < currentIndex || (status === step.key);
            const isCurrent = status === step.key;
            const muted = !isReached && !isCurrent;

            return (
              <div key={step.key} className="flex flex-1 items-start gap-3 md:flex-col md:items-center md:text-center" role="listitem">
                <div className="flex items-center gap-3 md:w-full md:justify-center">
                  {isReached ? (
                    <CheckCircle2 className="h-5 w-5 text-emerald-600" aria-hidden="true" />
                  ) : isCurrent ? (
                    <Circle className="h-5 w-5 text-blue-600" aria-hidden="true" />
                  ) : (
                    <Circle className="h-5 w-5 text-muted-foreground" aria-hidden="true" />
                  )}
                  {index < baseSteps.length - 1 ? (
                    <div className={`hidden h-px flex-1 md:block ${muted ? "bg-border" : "bg-primary/40"}`} aria-hidden="true" />
                  ) : null}
                </div>
                <div className="min-w-0">
                  <p className={`text-sm font-medium ${muted ? "text-muted-foreground" : "text-foreground"}`}>{step.label}</p>
                  <p className="text-xs text-muted-foreground">{formatTimestamp(timestamps[step.timestampKey])}</p>
                </div>
              </div>
            );
          })}
        </div>
        {status === "CANCELLED" || status === "DISPUTED" ? (
          <div className={`flex items-center gap-3 rounded-lg border px-3 py-3 text-sm ${status === "CANCELLED" ? "border-red-200 bg-red-50" : "border-amber-200 bg-amber-50"}`}>
            {status === "CANCELLED" ? (
              <XCircle className="h-4 w-4 text-red-600" aria-hidden="true" />
            ) : (
              <AlertTriangle className="h-4 w-4 text-amber-700" aria-hidden="true" />
            )}
            <div className="min-w-0">
              <p className="font-medium">{status === "CANCELLED" ? "Cancelled" : "Disputed"}</p>
              <p className="text-xs text-muted-foreground">{formatTimestamp(terminalTimestamp)}</p>
            </div>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}

export function EscrowTimelineSkeleton() {
  return (
    <Card aria-label="Loading escrow timeline">
      <CardHeader className="gap-3 md:flex md:flex-row md:items-start md:justify-between">
        <div className="space-y-2">
          <Skeleton className="h-5 w-36" />
          <Skeleton className="h-4 w-72" />
        </div>
        <Skeleton className="h-7 w-24 rounded-full" />
      </CardHeader>
      <CardContent>
        <div className="grid gap-4 md:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="space-y-2">
              <Skeleton className="h-5 w-5 rounded-full" />
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-3 w-24" />
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
