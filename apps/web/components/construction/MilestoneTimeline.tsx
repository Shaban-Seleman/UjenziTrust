"use client";

import { AlertTriangle, CheckCircle2, Clock3, Landmark, ShieldCheck } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import type { AdaptedMilestone } from "@/lib/adapters/milestone";
import { formatMoney } from "@/lib/utils/money";

type MilestoneTimelineProps = {
  projectId: string;
  milestones: AdaptedMilestone[];
};

const statusProgress: Record<string, number> = {
  PLANNED: 1,
  IN_PROGRESS: 2,
  SUBMITTED: 3,
  INSPECTED: 4,
  PASSED: 4,
  APPROVED: 5,
  PAID: 6,
  FAILED: 3,
  DISPUTED: 3
};

function statusMeta(status: string) {
  switch (status) {
    case "IN_PROGRESS":
      return { label: "In progress", className: "bg-blue-100 text-blue-800" };
    case "SUBMITTED":
      return { label: "Submitted", className: "bg-sky-100 text-sky-800" };
    case "INSPECTED":
    case "PASSED":
      return { label: "Inspected", className: "bg-indigo-100 text-indigo-800" };
    case "APPROVED":
      return { label: "Approved", className: "bg-violet-100 text-violet-800" };
    case "PAID":
      return { label: "Paid", className: "bg-emerald-100 text-emerald-800" };
    case "FAILED":
      return { label: "Failed", className: "bg-red-100 text-red-700" };
    case "DISPUTED":
      return { label: "Disputed", className: "bg-amber-100 text-amber-800" };
    default:
      return { label: "Planned", className: "bg-muted text-foreground" };
  }
}

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

function retentionCountdown(target?: string) {
  if (!target) {
    return "\u2014";
  }

  const releaseDate = new Date(target);
  if (Number.isNaN(releaseDate.getTime())) {
    return "\u2014";
  }

  const diffMs = releaseDate.getTime() - Date.now();
  if (diffMs <= 0) {
    return "Ready to release";
  }

  const days = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
  return `Releases in ${days} day${days === 1 ? "" : "s"}`;
}

function progressWidth(status: string) {
  const progress = statusProgress[status] ?? 1;
  return `${Math.max(16, Math.min(100, Math.round((progress / 6) * 100)))}%`;
}

function disbursementSummary(milestone: AdaptedMilestone) {
  if (milestone.totalDisbursements === undefined && milestone.settledDisbursements === undefined) {
    return "\u2014";
  }

  const settled = milestone.settledDisbursements ?? 0;
  const total = milestone.totalDisbursements ?? settled;
  return `${settled}/${total} Settled`;
}

export function MilestoneTimeline({ projectId, milestones }: MilestoneTimelineProps) {
  if (!milestones.length) {
    return (
      <Card aria-label={`Milestones for project ${projectId}`}>
        <CardHeader>
          <CardTitle>Milestone Timeline</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">No milestones yet.</CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-4" aria-label={`Milestones for project ${projectId}`}>
      <div className="space-y-1">
        <h2 className="text-lg font-semibold">Milestone Timeline</h2>
        <p className="text-sm text-muted-foreground">
          Milestone marked PAID only after all disbursements settle. Retention is held until the release date.
        </p>
      </div>
      <div className="space-y-4">
        {milestones.map((milestone, index) => {
          const meta = statusMeta(milestone.status);
          const net = milestone.grossAmount !== undefined
            ? Math.max(0, milestone.grossAmount - (milestone.retentionAmount ?? 0))
            : undefined;

          return (
            <div key={milestone.id} className="flex gap-3">
              <div className="flex flex-col items-center pt-6" aria-hidden="true">
                <div className="flex h-8 w-8 items-center justify-center rounded-full border border-border bg-background">
                  <CheckCircle2 className={`h-4 w-4 ${milestone.status === "PAID" ? "text-emerald-600" : "text-muted-foreground"}`} />
                </div>
                {index < milestones.length - 1 ? <div className="mt-2 h-full w-px bg-border" /> : null}
              </div>
              <Card className="flex-1">
                <CardHeader className="gap-3 md:flex md:flex-row md:items-start md:justify-between">
                  <div>
                    <CardTitle>{`Milestone ${milestone.sequence} \u2014 ${milestone.name}`}</CardTitle>
                    <p className="mt-1 text-xs text-muted-foreground">Paid at: {formatTimestamp(milestone.paidAt)}</p>
                  </div>
                  <Badge className={meta.className}>{meta.label}</Badge>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <div className="h-2 w-full overflow-hidden rounded-full bg-muted" aria-hidden="true">
                      <div className="h-full rounded-full bg-primary/70" style={{ width: progressWidth(milestone.status) }} />
                    </div>
                    <div className="grid gap-3 text-sm md:grid-cols-3">
                      <div>
                        <p className="text-xs text-muted-foreground">Gross</p>
                        <p className="font-medium">{milestone.grossAmount !== undefined ? formatMoney(milestone.grossAmount, "TZS") : "\u2014"}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">Retention</p>
                        <p className="font-medium">{milestone.retentionAmount !== undefined ? formatMoney(milestone.retentionAmount, "TZS") : "\u2014"}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">Net</p>
                        <p className="font-medium">{net !== undefined ? formatMoney(net, "TZS") : "\u2014"}</p>
                      </div>
                    </div>
                  </div>

                  <div className="grid gap-3 md:grid-cols-2">
                    <div className="rounded-lg border border-border p-3">
                      <div className="flex items-center gap-2">
                        <Landmark className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
                        <p className="text-sm font-medium">Payouts</p>
                      </div>
                      <p className="mt-2 text-sm text-muted-foreground">Disbursements: {disbursementSummary(milestone)}</p>
                      <div className="mt-2 flex flex-wrap gap-2">
                        {milestone.settledDisbursements !== undefined && milestone.totalDisbursements !== undefined ? (
                          milestone.settledDisbursements >= milestone.totalDisbursements ? (
                            <Badge className="bg-emerald-100 text-emerald-800">Settled</Badge>
                          ) : (
                            <Badge className="bg-slate-100 text-slate-800">Pending</Badge>
                          )
                        ) : (
                          <Badge variant="outline">Unknown</Badge>
                        )}
                        {milestone.hasFailures ? <Badge className="bg-red-100 text-red-700">Failed</Badge> : null}
                        {milestone.inspectionStatus ? <Badge className="bg-indigo-100 text-indigo-800">{milestone.inspectionStatus}</Badge> : null}
                      </div>
                    </div>

                    <div className="rounded-lg border border-border p-3">
                      <div className="flex items-center gap-2">
                        <ShieldCheck className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
                        <p className="text-sm font-medium">Retention</p>
                      </div>
                      <p className="mt-2 text-sm text-muted-foreground">
                        Retention held: {milestone.retentionAmount !== undefined ? formatMoney(milestone.retentionAmount, "TZS") : "\u2014"}
                      </p>
                      <div className="mt-2 flex items-center gap-2 text-sm">
                        <Clock3 className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
                        <span>{retentionCountdown(milestone.retentionReleaseAt)}</span>
                      </div>
                      <p className="mt-1 text-xs text-muted-foreground">Release date: {formatTimestamp(milestone.retentionReleaseAt)}</p>
                    </div>
                  </div>

                  {milestone.hasFailures ? (
                    <div className="flex items-center gap-2 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                      <AlertTriangle className="h-4 w-4" aria-hidden="true" />
                      One or more disbursements failed and may need operator review.
                    </div>
                  ) : null}
                </CardContent>
              </Card>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export function MilestoneTimelineSkeleton() {
  return (
    <div className="space-y-4" aria-label="Loading milestone timeline">
      <div className="space-y-2">
        <Skeleton className="h-6 w-40" />
        <Skeleton className="h-4 w-80" />
      </div>
      {Array.from({ length: 3 }).map((_, index) => (
        <div key={index} className="flex gap-3">
          <div className="flex flex-col items-center pt-6">
            <Skeleton className="h-8 w-8 rounded-full" />
            <Skeleton className="mt-2 h-24 w-px" />
          </div>
          <Card className="flex-1">
            <CardHeader className="gap-3 md:flex md:flex-row md:items-start md:justify-between">
              <div className="space-y-2">
                <Skeleton className="h-5 w-52" />
                <Skeleton className="h-4 w-36" />
              </div>
              <Skeleton className="h-7 w-20 rounded-full" />
            </CardHeader>
            <CardContent className="space-y-4">
              <Skeleton className="h-2 w-full" />
              <div className="grid gap-3 md:grid-cols-3">
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
              </div>
              <div className="grid gap-3 md:grid-cols-2">
                <Skeleton className="h-24 w-full" />
                <Skeleton className="h-24 w-full" />
              </div>
            </CardContent>
          </Card>
        </div>
      ))}
    </div>
  );
}
