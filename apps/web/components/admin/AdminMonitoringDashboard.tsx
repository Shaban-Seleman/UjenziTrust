"use client";

import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { toast } from "sonner";
import { RoleGate } from "@/components/auth/RoleGate";
import { DemoQuickStartCard } from "@/components/demo/DemoQuickStartCard";
import { DemoSetupButton } from "@/components/demo/DemoSetupButton";
import { DataTable } from "@/components/tables/DataTable";
import { TablePagination } from "@/components/tables/TablePagination";
import { TableSkeleton } from "@/components/tables/TableSkeleton";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  getAdminDisbursement,
  getLedgerEntry,
  getOperatorAuditEvent,
  getOutboxEvent,
  getSystemHealth,
  getWebhookEvent,
  listAdminDisbursementsPage,
  listLedgerEntriesAdminPage,
  listOperatorAuditEventsPage,
  listOutboxEventsAdminPage,
  listWebhookEventsAdminPage,
  retryOutboxEvent
} from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/errors";
import type {
  Disbursement,
  LedgerEntry,
  LedgerEntryDetail,
  OperatorAudit,
  OperatorAuditDetail,
  OutboxEvent,
  OutboxEventDetail,
  SystemHealth,
  WebhookEvent,
  WebhookEventDetail
} from "@/lib/api/schemas";
import { formatDateTime } from "@/lib/utils/dates";
import { formatEntityLabel, formatShortId } from "@/lib/utils/labels";
import { formatMoney } from "@/lib/utils/money";

const disbursementStatuses = ["ALL", "PENDING", "SUBMITTED", "SETTLED", "FAILED", "CANCELLED"] as const;
const payeeTypes = ["ALL", "CONTRACTOR", "SUPPLIER", "INSPECTOR", "RETENTION"] as const;
const outboxStatuses = ["ALL", "PENDING", "SENT", "FAILED"] as const;
const webhookStatuses = ["ALL", "RECEIVED", "PROCESSED", "FAILED"] as const;
const eventTypes = ["ALL", "PAYOUT_REQUESTED", "DISBURSEMENT_SETTLED"] as const;
const auditOutcomes = ["ALL", "SUCCESS", "FAILED", "FORBIDDEN", "NOOP"] as const;
const auditActions = ["ALL", "OUTBOX_EVENT_RETRY", "DEMO_RESET", "DEMO_SEED", "DEMO_RESET_AND_SEED", "PROJECT_ASSIGN_PARTICIPANTS", "PROJECT_ACTIVATED", "MILESTONE_CREATED", "MILESTONE_APPROVE_MULTI", "MILESTONE_RETENTION_RELEASE", "OPERATOR_ACTION_FAILED", "ADMIN_FORBIDDEN_ACCESS_ATTEMPT"] as const;

function errorDetail(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.detail : fallback;
}

function tone(status?: string | null) {
  switch ((status ?? "").toUpperCase()) {
    case "UP":
    case "PROCESSED":
    case "SETTLED":
    case "SENT":
      return "bg-emerald-100 text-emerald-900";
    case "FAILED":
    case "DOWN":
      return "bg-red-100 text-red-900";
    case "PENDING":
    case "RECEIVED":
    case "SUBMITTED":
      return "bg-amber-100 text-amber-900";
    default:
      return "bg-muted text-foreground";
  }
}

function JsonView({ value }: { value: unknown }) {
  return (
    <pre className="max-h-80 overflow-auto rounded-md bg-muted p-3 text-xs">
      {JSON.stringify(value ?? null, null, 2)}
    </pre>
  );
}

function AdminSectionHeader({
  title,
  description,
  children
}: {
  title: string;
  description: string;
  children?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
      <div className="space-y-1">
        <h2 className="text-lg font-semibold">{title}</h2>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
      {children}
    </div>
  );
}

export function AdminMonitoringDashboard() {
  const queryClient = useQueryClient();

  const [disbursementPage, setDisbursementPage] = useState(0);
  const [outboxPage, setOutboxPage] = useState(0);
  const [webhookPage, setWebhookPage] = useState(0);
  const [ledgerPage, setLedgerPage] = useState(0);
  const [auditPage, setAuditPage] = useState(0);
  const pageSize = 8;

  const [disbursementStatus, setDisbursementStatus] = useState<string>("ALL");
  const [payeeType, setPayeeType] = useState<string>("ALL");
  const [outboxStatus, setOutboxStatus] = useState<string>("ALL");
  const [outboxEventType, setOutboxEventType] = useState<string>("ALL");
  const [webhookStatus, setWebhookStatus] = useState<string>("ALL");
  const [webhookEventType, setWebhookEventType] = useState<string>("ALL");
  const [webhookProvider, setWebhookProvider] = useState<string>("ALL");
  const [ledgerEntryType, setLedgerEntryType] = useState("");
  const [ledgerReferenceId, setLedgerReferenceId] = useState("");
  const [auditActionType, setAuditActionType] = useState<string>("ALL");
  const [auditOutcome, setAuditOutcome] = useState<string>("ALL");

  const [selectedDisbursementId, setSelectedDisbursementId] = useState<string | null>(null);
  const [selectedOutboxId, setSelectedOutboxId] = useState<string | null>(null);
  const [selectedWebhookId, setSelectedWebhookId] = useState<string | null>(null);
  const [selectedLedgerEntryId, setSelectedLedgerEntryId] = useState<string | null>(null);
  const [selectedAuditId, setSelectedAuditId] = useState<string | null>(null);
  const [retryConfirmId, setRetryConfirmId] = useState<string | null>(null);

  const systemHealth = useQuery({ queryKey: ["admin", "system-health"], queryFn: getSystemHealth });
  const disbursements = useQuery({
    queryKey: ["admin", "disbursements", disbursementPage, pageSize, disbursementStatus, payeeType],
    queryFn: () => listAdminDisbursementsPage(disbursementPage, pageSize, {
      status: disbursementStatus === "ALL" ? undefined : disbursementStatus,
      payeeType: payeeType === "ALL" ? undefined : payeeType
    })
  });
  const outbox = useQuery({
    queryKey: ["admin", "outbox", outboxPage, pageSize, outboxStatus, outboxEventType],
    queryFn: () => listOutboxEventsAdminPage(outboxPage, pageSize, {
      status: outboxStatus === "ALL" ? undefined : outboxStatus,
      eventType: outboxEventType === "ALL" ? undefined : outboxEventType
    })
  });
  const webhooks = useQuery({
    queryKey: ["admin", "webhooks", webhookPage, pageSize, webhookStatus, webhookEventType, webhookProvider],
    queryFn: () => listWebhookEventsAdminPage(webhookPage, pageSize, {
      status: webhookStatus === "ALL" ? undefined : webhookStatus,
      eventType: webhookEventType === "ALL" ? undefined : webhookEventType,
      source: webhookProvider === "ALL" ? undefined : webhookProvider
    })
  });
  const ledger = useQuery({
    queryKey: ["admin", "ledger", ledgerPage, pageSize, ledgerEntryType, ledgerReferenceId],
    queryFn: () => listLedgerEntriesAdminPage(ledgerPage, pageSize, {
      entryType: ledgerEntryType || undefined,
      referenceId: ledgerReferenceId || undefined
    })
  });
  const audit = useQuery({
    queryKey: ["admin", "audit", auditPage, pageSize, auditActionType, auditOutcome],
    queryFn: () => listOperatorAuditEventsPage(auditPage, pageSize, {
      actionType: auditActionType === "ALL" ? undefined : auditActionType,
      outcome: auditOutcome === "ALL" ? undefined : auditOutcome
    })
  });

  const disbursementDetail = useQuery({
    queryKey: ["admin", "disbursement", selectedDisbursementId],
    queryFn: () => getAdminDisbursement(selectedDisbursementId!),
    enabled: !!selectedDisbursementId
  });
  const outboxDetail = useQuery({
    queryKey: ["admin", "outbox", "detail", selectedOutboxId],
    queryFn: () => getOutboxEvent(selectedOutboxId!),
    enabled: !!selectedOutboxId
  });
  const webhookDetail = useQuery({
    queryKey: ["admin", "webhook", "detail", selectedWebhookId],
    queryFn: () => getWebhookEvent(selectedWebhookId!),
    enabled: !!selectedWebhookId
  });
  const ledgerDetail = useQuery({
    queryKey: ["admin", "ledger", "detail", selectedLedgerEntryId],
    queryFn: () => getLedgerEntry(selectedLedgerEntryId!),
    enabled: !!selectedLedgerEntryId
  });
  const auditDetail = useQuery({
    queryKey: ["admin", "audit", "detail", selectedAuditId],
    queryFn: () => getOperatorAuditEvent(selectedAuditId!),
    enabled: !!selectedAuditId
  });

  const retryMutation = useMutation({
    mutationFn: (id: string) => retryOutboxEvent(id),
    onSuccess: async () => {
      toast.success("Outbox event re-queued");
      setRetryConfirmId(null);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["admin", "outbox"] }),
        queryClient.invalidateQueries({ queryKey: ["admin", "system-health"] })
      ]);
    },
    onError: (error) => toast.error(errorDetail(error, "Retry failed"))
  });

  const disbursementColumns = useMemo<ColumnDef<Disbursement>[]>(() => [
    { header: "Disbursement", accessorKey: "id", cell: ({ row }) => formatEntityLabel("Disb", row.original.id) },
    { header: "Business Key", accessorKey: "businessKey" },
    { header: "Payee", accessorKey: "payeeType", cell: ({ row }) => `${row.original.payeeType} • ${formatShortId(String(row.original.payeeId))}` },
    { header: "Amount", accessorKey: "amount", cell: ({ row }) => formatMoney(row.original.amount, row.original.currency ?? "TZS") },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge className={tone(row.original.status)}>{row.original.status}</Badge> },
    { header: "Milestone", accessorKey: "milestoneId", cell: ({ row }) => formatEntityLabel("Milestone", row.original.milestoneId) },
    { header: "Escrow", accessorKey: "escrowId", cell: ({ row }) => formatEntityLabel("Escrow", row.original.escrowId) },
    { header: "Action", cell: ({ row }) => <Button size="sm" variant="outline" onClick={() => setSelectedDisbursementId(row.original.id)}>View</Button> }
  ], []);

  const outboxColumns = useMemo<ColumnDef<OutboxEvent>[]>(() => [
    { header: "Event Type", accessorKey: "eventType" },
    { header: "Aggregate", accessorKey: "aggregateType", cell: ({ row }) => `${row.original.aggregateType ?? "—"} • ${formatShortId(row.original.aggregateId)}` },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge className={tone(row.original.status)}>{row.original.status}</Badge> },
    { header: "Attempts", accessorKey: "attempts", cell: ({ row }) => row.original.attempts ?? 0 },
    { header: "Next Attempt", accessorKey: "nextAttemptAt", cell: ({ row }) => formatDateTime(row.original.nextAttemptAt) },
    { header: "Action", cell: ({ row }) => <Button size="sm" variant="outline" onClick={() => setSelectedOutboxId(row.original.id)}>View</Button> }
  ], []);

  const webhookColumns = useMemo<ColumnDef<WebhookEvent>[]>(() => [
    { header: "Event ID", accessorKey: "eventId", cell: ({ row }) => formatEntityLabel("Event", row.original.eventId) },
    { header: "Provider", accessorKey: "provider", cell: ({ row }) => row.original.provider ?? "—" },
    { header: "Type", accessorKey: "eventType", cell: ({ row }) => row.original.eventType ?? "—" },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge className={tone(row.original.status)}>{row.original.status}</Badge> },
    { header: "Received", accessorKey: "receivedAt", cell: ({ row }) => formatDateTime(row.original.receivedAt) },
    { header: "Action", cell: ({ row }) => <Button size="sm" variant="outline" onClick={() => setSelectedWebhookId(row.original.id)}>View</Button> }
  ], []);

  const ledgerColumns = useMemo<ColumnDef<LedgerEntry>[]>(() => [
    { header: "Entry", accessorKey: "entryType" },
    { header: "Reference", accessorKey: "referenceId", cell: ({ row }) => formatEntityLabel("Ref", row.original.referenceId) },
    { header: "Idempotency", accessorKey: "idempotencyKey", cell: ({ row }) => formatShortId(row.original.idempotencyKey) },
    { header: "Hash", accessorKey: "hash", cell: ({ row }) => formatShortId(row.original.hash) },
    { header: "Created", accessorKey: "createdAt", cell: ({ row }) => formatDateTime(row.original.createdAt) },
    { header: "Action", cell: ({ row }) => <Button size="sm" variant="outline" onClick={() => setSelectedLedgerEntryId(row.original.id)}>View</Button> }
  ], []);
  const auditColumns = useMemo<ColumnDef<OperatorAudit>[]>(() => [
    { header: "Timestamp", accessorKey: "createdAt", cell: ({ row }) => formatDateTime(row.original.createdAt) },
    { header: "Actor", accessorKey: "actorUserId", cell: ({ row }) => formatEntityLabel("User", row.original.actorUserId) },
    { header: "Action", accessorKey: "actionType" },
    { header: "Resource", accessorKey: "resourceType", cell: ({ row }) => `${row.original.resourceType} • ${row.original.resourceId ?? "—"}` },
    { header: "Outcome", accessorKey: "outcome", cell: ({ row }) => <Badge className={tone(row.original.outcome)}>{row.original.outcome}</Badge> },
    { header: "Action", cell: ({ row }) => <Button size="sm" variant="outline" onClick={() => setSelectedAuditId(row.original.id)}>View</Button> }
  ], []);

  const health = systemHealth.data;

  return (
    <RoleGate roles={["ADMIN"]} fallback={<p className="text-sm text-red-700">Not authorized.</p>}>
      <div className="space-y-6">
        <div className="space-y-2">
          <h1 className="text-2xl font-semibold">Admin Console</h1>
          <p className="text-sm text-muted-foreground">
            Operational visibility for disbursements, queues, settlement webhooks, ledger entries, and system health.
          </p>
        </div>

        <div className="grid gap-4 lg:grid-cols-[1.4fr,1fr]">
          <div className="space-y-4">
            <DemoSetupButton />
            <DemoQuickStartCard />
          </div>
          <Card>
            <CardHeader>
              <CardTitle>System Snapshot</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3 sm:grid-cols-2">
              {systemHealth.isLoading ? (
                <TableSkeleton rows={2} />
              ) : systemHealth.isError || !health ? (
                <p className="text-sm text-red-700">System health endpoint unavailable.</p>
              ) : (
                <>
                  <div className="rounded-md border p-3">
                    <p className="text-xs text-muted-foreground">Overall Health</p>
                    <Badge className={tone(health.overallStatus)}>{health.overallStatus}</Badge>
                  </div>
                  <div className="rounded-md border p-3">
                    <p className="text-xs text-muted-foreground">Database</p>
                    <Badge className={tone(health.databaseStatus)}>{health.databaseStatus}</Badge>
                  </div>
                  <div className="rounded-md border p-3">
                    <p className="text-xs text-muted-foreground">Pending Outbox</p>
                    <p className="text-lg font-semibold">{health.queue.pendingOutboxEvents}</p>
                  </div>
                  <div className="rounded-md border p-3">
                    <p className="text-xs text-muted-foreground">Failed Outbox</p>
                    <p className="text-lg font-semibold">{health.queue.failedOutboxEvents}</p>
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        </div>

        <Tabs defaultValue="disbursements" className="space-y-4">
          <TabsList className="flex w-full flex-wrap justify-start gap-1">
            <TabsTrigger value="disbursements">Disbursements</TabsTrigger>
            <TabsTrigger value="outbox">Outbox</TabsTrigger>
            <TabsTrigger value="webhooks">Webhooks</TabsTrigger>
            <TabsTrigger value="ledger">Ledger</TabsTrigger>
            <TabsTrigger value="audit">Operator Audit</TabsTrigger>
            <TabsTrigger value="system">System Health</TabsTrigger>
          </TabsList>

          <TabsContent value="disbursements" className="space-y-4">
            <Card>
              <CardHeader>
                <AdminSectionHeader title="Disbursement Monitoring" description="Inspect payout execution state across escrows and milestones.">
                  <div className="grid gap-2 sm:grid-cols-2">
                    <Select value={disbursementStatus} onValueChange={(value) => { setDisbursementStatus(value); setDisbursementPage(0); }}>
                      <SelectTrigger><SelectValue placeholder="Status" /></SelectTrigger>
                      <SelectContent>{disbursementStatuses.map((value) => <SelectItem key={value} value={value}>{value}</SelectItem>)}</SelectContent>
                    </Select>
                    <Select value={payeeType} onValueChange={(value) => { setPayeeType(value); setDisbursementPage(0); }}>
                      <SelectTrigger><SelectValue placeholder="Payee Type" /></SelectTrigger>
                      <SelectContent>{payeeTypes.map((value) => <SelectItem key={value} value={value}>{value}</SelectItem>)}</SelectContent>
                    </Select>
                  </div>
                </AdminSectionHeader>
              </CardHeader>
              <CardContent>
                {disbursements.isLoading ? (
                  <TableSkeleton />
                ) : (
                  <div className="space-y-4">
                    <DataTable columns={disbursementColumns} data={disbursements.data?.content ?? []} emptyLabel={disbursements.isError ? "Disbursement endpoint unavailable" : "No disbursements found"} />
                    <TablePagination
                      page={disbursementPage}
                      size={pageSize}
                      totalElements={disbursements.data?.totalElements ?? 0}
                      totalPages={disbursements.data?.totalPages ?? 0}
                      onPrevious={() => setDisbursementPage((value) => Math.max(0, value - 1))}
                      onNext={() => setDisbursementPage((value) => value + 1)}
                    />
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="outbox" className="space-y-4">
            <Card>
              <CardHeader>
                <AdminSectionHeader title="Outbox Events" description="Investigate async payout dispatch and safely re-queue terminal failures.">
                  <div className="grid gap-2 sm:grid-cols-2">
                    <Select value={outboxStatus} onValueChange={(value) => { setOutboxStatus(value); setOutboxPage(0); }}>
                      <SelectTrigger><SelectValue placeholder="Status" /></SelectTrigger>
                      <SelectContent>{outboxStatuses.map((value) => <SelectItem key={value} value={value}>{value}</SelectItem>)}</SelectContent>
                    </Select>
                    <Select value={outboxEventType} onValueChange={(value) => { setOutboxEventType(value); setOutboxPage(0); }}>
                      <SelectTrigger><SelectValue placeholder="Event Type" /></SelectTrigger>
                      <SelectContent>{eventTypes.map((value) => <SelectItem key={value} value={value}>{value}</SelectItem>)}</SelectContent>
                    </Select>
                  </div>
                </AdminSectionHeader>
              </CardHeader>
              <CardContent>
                {outbox.isLoading ? (
                  <TableSkeleton />
                ) : (
                  <div className="space-y-4">
                    <DataTable columns={outboxColumns} data={outbox.data?.content ?? []} emptyLabel={outbox.isError ? "Outbox endpoint unavailable" : "No outbox events"} />
                    <TablePagination
                      page={outboxPage}
                      size={pageSize}
                      totalElements={outbox.data?.totalElements ?? 0}
                      totalPages={outbox.data?.totalPages ?? 0}
                      onPrevious={() => setOutboxPage((value) => Math.max(0, value - 1))}
                      onNext={() => setOutboxPage((value) => value + 1)}
                    />
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="webhooks" className="space-y-4">
            <Card>
              <CardHeader>
                <AdminSectionHeader title="Webhook Events" description="Inspect payment provider delivery history and processing outcomes.">
                  <div className="grid gap-2 sm:grid-cols-3">
                    <Select value={webhookProvider} onValueChange={(value) => { setWebhookProvider(value); setWebhookPage(0); }}>
                      <SelectTrigger><SelectValue placeholder="Provider" /></SelectTrigger>
                      <SelectContent>{["ALL", "MOCK_BANK"].map((value) => <SelectItem key={value} value={value}>{value}</SelectItem>)}</SelectContent>
                    </Select>
                    <Select value={webhookStatus} onValueChange={(value) => { setWebhookStatus(value); setWebhookPage(0); }}>
                      <SelectTrigger><SelectValue placeholder="Status" /></SelectTrigger>
                      <SelectContent>{webhookStatuses.map((value) => <SelectItem key={value} value={value}>{value}</SelectItem>)}</SelectContent>
                    </Select>
                    <Select value={webhookEventType} onValueChange={(value) => { setWebhookEventType(value); setWebhookPage(0); }}>
                      <SelectTrigger><SelectValue placeholder="Event Type" /></SelectTrigger>
                      <SelectContent>{eventTypes.map((value) => <SelectItem key={value} value={value}>{value}</SelectItem>)}</SelectContent>
                    </Select>
                  </div>
                </AdminSectionHeader>
              </CardHeader>
              <CardContent>
                {webhooks.isLoading ? (
                  <TableSkeleton />
                ) : (
                  <div className="space-y-4">
                    <DataTable columns={webhookColumns} data={webhooks.data?.content ?? []} emptyLabel={webhooks.isError ? "Webhook endpoint unavailable" : "No webhook events"} />
                    <TablePagination
                      page={webhookPage}
                      size={pageSize}
                      totalElements={webhooks.data?.totalElements ?? 0}
                      totalPages={webhooks.data?.totalPages ?? 0}
                      onPrevious={() => setWebhookPage((value) => Math.max(0, value - 1))}
                      onNext={() => setWebhookPage((value) => value + 1)}
                    />
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="ledger" className="space-y-4">
            <Card>
              <CardHeader>
                <AdminSectionHeader title="Ledger Monitoring" description="Inspect journal entries and lines without database access.">
                  <div className="grid gap-2 sm:grid-cols-2">
                    <Input
                      placeholder="Entry type"
                      value={ledgerEntryType}
                      onChange={(event) => { setLedgerEntryType(event.target.value); setLedgerPage(0); }}
                    />
                    <Input
                      placeholder="Reference id"
                      value={ledgerReferenceId}
                      onChange={(event) => { setLedgerReferenceId(event.target.value); setLedgerPage(0); }}
                    />
                  </div>
                </AdminSectionHeader>
              </CardHeader>
              <CardContent>
                {ledger.isLoading ? (
                  <TableSkeleton />
                ) : (
                  <div className="space-y-4">
                    <DataTable columns={ledgerColumns} data={ledger.data?.content ?? []} emptyLabel={ledger.isError ? "Ledger endpoint unavailable" : "No ledger entries"} />
                    <TablePagination
                      page={ledgerPage}
                      size={pageSize}
                      totalElements={ledger.data?.totalElements ?? 0}
                      totalPages={ledger.data?.totalPages ?? 0}
                      onPrevious={() => setLedgerPage((value) => Math.max(0, value - 1))}
                      onNext={() => setLedgerPage((value) => value + 1)}
                    />
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="system" className="space-y-4">
            <Card>
              <CardHeader>
                <AdminSectionHeader title="System Health" description="Background job heartbeat, queue pressure, and docs exposure state." />
              </CardHeader>
              <CardContent>
                {systemHealth.isLoading ? (
                  <TableSkeleton />
                ) : systemHealth.isError || !health ? (
                  <p className="text-sm text-red-700">System health endpoint unavailable.</p>
                ) : (
                  <div className="space-y-4">
                    <div className="grid gap-4 md:grid-cols-4">
                      <Card><CardHeader><CardTitle className="text-sm">Overall</CardTitle></CardHeader><CardContent><Badge className={tone(health.overallStatus)}>{health.overallStatus}</Badge></CardContent></Card>
                      <Card><CardHeader><CardTitle className="text-sm">Database</CardTitle></CardHeader><CardContent><Badge className={tone(health.databaseStatus)}>{health.databaseStatus}</Badge></CardContent></Card>
                      <Card><CardHeader><CardTitle className="text-sm">Scheduling</CardTitle></CardHeader><CardContent><Badge className={tone(health.schedulingEnabled ? "UP" : "DOWN")}>{health.schedulingEnabled ? "ENABLED" : "DISABLED"}</Badge></CardContent></Card>
                      <Card><CardHeader><CardTitle className="text-sm">Swagger Docs</CardTitle></CardHeader><CardContent><Badge className={tone(health.docsEnabled ? "UP" : "DOWN")}>{health.docsEnabled ? "ENABLED" : "DISABLED"}</Badge></CardContent></Card>
                    </div>
                    <div className="grid gap-4 md:grid-cols-2">
                      <Card><CardHeader><CardTitle className="text-sm">Pending Outbox</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{health.queue.pendingOutboxEvents}</CardContent></Card>
                      <Card><CardHeader><CardTitle className="text-sm">Failed Outbox</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{health.queue.failedOutboxEvents}</CardContent></Card>
                    </div>
                    <div className="rounded-md border">
                      <table className="w-full text-sm">
                        <thead className="bg-muted/50 text-left">
                          <tr>
                            <th className="p-3">Job</th>
                            <th className="p-3">Schedule</th>
                            <th className="p-3">Last Success</th>
                            <th className="p-3">Last Error</th>
                          </tr>
                        </thead>
                        <tbody>
                          {Object.values(health.jobs).map((job) => (
                            <tr key={job.name} className="border-t">
                              <td className="p-3">{job.name}</td>
                              <td className="p-3 text-muted-foreground">{job.schedule}</td>
                              <td className="p-3">{formatDateTime(job.lastSuccessAt)}</td>
                              <td className="p-3 text-red-700">{job.lastError ?? "—"}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="audit" className="space-y-4">
            <Card>
              <CardHeader>
                <AdminSectionHeader title="Operator Audit Trail" description="Persistent record of sensitive operator and admin actions.">
                  <div className="grid gap-2 sm:grid-cols-2">
                    <Select value={auditActionType} onValueChange={(value) => { setAuditActionType(value); setAuditPage(0); }}>
                      <SelectTrigger><SelectValue placeholder="Action Type" /></SelectTrigger>
                      <SelectContent>{auditActions.map((value) => <SelectItem key={value} value={value}>{value}</SelectItem>)}</SelectContent>
                    </Select>
                    <Select value={auditOutcome} onValueChange={(value) => { setAuditOutcome(value); setAuditPage(0); }}>
                      <SelectTrigger><SelectValue placeholder="Outcome" /></SelectTrigger>
                      <SelectContent>{auditOutcomes.map((value) => <SelectItem key={value} value={value}>{value}</SelectItem>)}</SelectContent>
                    </Select>
                  </div>
                </AdminSectionHeader>
              </CardHeader>
              <CardContent>
                {audit.isLoading ? (
                  <TableSkeleton />
                ) : (
                  <div className="space-y-4">
                    <DataTable columns={auditColumns} data={audit.data?.content ?? []} emptyLabel={audit.isError ? "Audit endpoint unavailable" : "No audit events"} />
                    <TablePagination
                      page={auditPage}
                      size={pageSize}
                      totalElements={audit.data?.totalElements ?? 0}
                      totalPages={audit.data?.totalPages ?? 0}
                      onPrevious={() => setAuditPage((value) => Math.max(0, value - 1))}
                      onNext={() => setAuditPage((value) => value + 1)}
                    />
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>

        <Dialog open={!!selectedDisbursementId} onOpenChange={(open) => !open && setSelectedDisbursementId(null)}>
          <DialogContent className="max-w-2xl">
            <DialogHeader><DialogTitle>Disbursement Detail</DialogTitle></DialogHeader>
            <EntityState query={disbursementDetail}>
              {(detail) => <DisbursementDetail detail={detail} />}
            </EntityState>
          </DialogContent>
        </Dialog>

        <Dialog open={!!selectedOutboxId} onOpenChange={(open) => !open && setSelectedOutboxId(null)}>
          <DialogContent className="max-w-3xl">
            <DialogHeader><DialogTitle>Outbox Event Detail</DialogTitle></DialogHeader>
            <EntityState query={outboxDetail}>
              {(detail) => (
                <OutboxDetail
                  detail={detail}
                  onRetry={() => setRetryConfirmId(detail.id)}
                />
              )}
            </EntityState>
          </DialogContent>
        </Dialog>

        <Dialog open={!!retryConfirmId} onOpenChange={(open) => !open && setRetryConfirmId(null)}>
          <DialogContent className="max-w-md">
            <DialogHeader><DialogTitle>Retry Outbox Event</DialogTitle></DialogHeader>
            <p className="text-sm text-muted-foreground">
              This will re-queue the failed outbox event for dispatch. Downstream idempotency remains responsible for preventing duplicate side effects.
            </p>
            <div className="flex gap-2">
              <Button variant="outline" onClick={() => setRetryConfirmId(null)}>Cancel</Button>
              <Button onClick={() => retryConfirmId && retryMutation.mutate(retryConfirmId)} disabled={retryMutation.isPending}>
                {retryMutation.isPending ? "Retrying..." : "Confirm Retry"}
              </Button>
            </div>
          </DialogContent>
        </Dialog>

        <Dialog open={!!selectedWebhookId} onOpenChange={(open) => !open && setSelectedWebhookId(null)}>
          <DialogContent className="max-w-3xl">
            <DialogHeader><DialogTitle>Webhook Event Detail</DialogTitle></DialogHeader>
            <EntityState query={webhookDetail}>
              {(detail) => <WebhookDetail detail={detail} />}
            </EntityState>
          </DialogContent>
        </Dialog>

        <Dialog open={!!selectedLedgerEntryId} onOpenChange={(open) => !open && setSelectedLedgerEntryId(null)}>
          <DialogContent className="max-w-4xl">
            <DialogHeader><DialogTitle>Ledger Entry Detail</DialogTitle></DialogHeader>
            <EntityState query={ledgerDetail}>
              {(detail) => <LedgerDetail detail={detail} />}
            </EntityState>
          </DialogContent>
        </Dialog>

        <Dialog open={!!selectedAuditId} onOpenChange={(open) => !open && setSelectedAuditId(null)}>
          <DialogContent className="max-w-3xl">
            <DialogHeader><DialogTitle>Operator Audit Event</DialogTitle></DialogHeader>
            <EntityState query={auditDetail}>
              {(detail) => <OperatorAuditDetailPanel detail={detail} />}
            </EntityState>
          </DialogContent>
        </Dialog>
      </div>
    </RoleGate>
  );
}

function EntityState<T>({
  query,
  children
}: {
  query: { isLoading: boolean; isError: boolean; error: unknown; data?: T };
  children: (data: T) => React.ReactNode;
}) {
  if (query.isLoading) {
    return <TableSkeleton rows={3} />;
  }
  if (query.isError || !query.data) {
    return <p className="text-sm text-red-700">{errorDetail(query.error, "Detail unavailable.")}</p>;
  }
  return <>{children(query.data)}</>;
}

function DisbursementDetail({ detail }: { detail: Disbursement }) {
  return (
    <div className="grid gap-3 text-sm md:grid-cols-2">
      <Info label="Disbursement ID" value={detail.id} mono />
      <Info label="Business Key" value={detail.businessKey} mono />
      <Info label="Status" value={detail.status} badge />
      <Info label="Payee" value={`${detail.payeeType} • ${detail.payeeId}`} mono />
      <Info label="Amount" value={formatMoney(detail.amount, detail.currency ?? "TZS")} />
      <Info label="Milestone" value={detail.milestoneId} mono />
      <Info label="Project" value={detail.projectId} mono />
      <Info label="Escrow" value={detail.escrowId} mono />
      <Info label="Bank Ref" value={detail.bankReference} mono />
      <Info label="Settlement Ref" value={detail.settlementRef} mono />
      <Info label="Created" value={formatDateTime(detail.createdAt)} />
      <Info label="Updated" value={formatDateTime(detail.updatedAt)} />
    </div>
  );
}

function OutboxDetail({ detail, onRetry }: { detail: OutboxEventDetail; onRetry: () => void }) {
  return (
    <div className="space-y-4 text-sm">
      <div className="grid gap-3 md:grid-cols-2">
        <Info label="Event ID" value={detail.id} mono />
        <Info label="Status" value={detail.status} badge />
        <Info label="Aggregate" value={`${detail.aggregateType ?? "—"} • ${detail.aggregateId ?? "—"}`} />
        <Info label="Event Type" value={detail.eventType} />
        <Info label="Idempotency Key" value={detail.idempotencyKey} mono />
        <Info label="Attempts" value={String(detail.attempts ?? 0)} />
        <Info label="Next Attempt" value={formatDateTime(detail.nextAttemptAt)} />
        <Info label="Last Error" value={detail.lastError} />
      </div>
      <div className="space-y-2">
        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Payload</p>
        <JsonView value={detail.payload} />
      </div>
      {detail.status === "FAILED" ? (
        <Button onClick={onRetry}>Retry Event</Button>
      ) : null}
    </div>
  );
}

function WebhookDetail({ detail }: { detail: WebhookEventDetail }) {
  return (
    <div className="space-y-4 text-sm">
      <div className="grid gap-3 md:grid-cols-2">
        <Info label="Webhook ID" value={detail.id} mono />
        <Info label="Provider" value={detail.provider} />
        <Info label="Event ID" value={detail.eventId} mono />
        <Info label="Status" value={detail.status} badge />
        <Info label="Type" value={detail.eventType} />
        <Info label="Disbursement" value={detail.relatedDisbursementId} mono />
        <Info label="Escrow" value={detail.relatedEscrowId} mono />
        <Info label="Milestone" value={detail.relatedMilestoneId} mono />
        <Info label="Settlement Ref" value={detail.relatedSettlementRef} mono />
        <Info label="Event Timestamp" value={formatDateTime(detail.eventTs)} />
        <Info label="Received" value={formatDateTime(detail.receivedAt)} />
        <Info label="Processed" value={formatDateTime(detail.processedAt)} />
      </div>
      <div className="space-y-2">
        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Payload</p>
        <JsonView value={detail.payload} />
      </div>
    </div>
  );
}

function LedgerDetail({ detail }: { detail: LedgerEntryDetail }) {
  return (
    <div className="space-y-4 text-sm">
      <div className="grid gap-3 md:grid-cols-2">
        <Info label="Entry ID" value={detail.id} mono />
        <Info label="Entry Type" value={detail.entryType} />
        <Info label="Reference ID" value={detail.referenceId} mono />
        <Info label="Idempotency Key" value={detail.idempotencyKey} mono />
        <Info label="Chain Index" value={detail.chainIndex ? String(detail.chainIndex) : "—"} />
        <Info label="Actor User" value={detail.actorUserId} mono />
        <Info label="Hash" value={detail.hash} mono />
        <Info label="Prev Hash" value={detail.prevHash} mono />
      </div>
      <div className="space-y-2">
        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Journal Lines</p>
        <div className="rounded-md border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50 text-left">
              <tr>
                <th className="p-3">Account</th>
                <th className="p-3">Direction</th>
                <th className="p-3">Amount</th>
              </tr>
            </thead>
            <tbody>
              {detail.lines.map((line) => (
                <tr key={line.id} className="border-t">
                  <td className="p-3">{line.accountCode} • {line.accountName}</td>
                  <td className="p-3">{line.direction}</td>
                  <td className="p-3">{formatMoney(line.amount, line.currency)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function OperatorAuditDetailPanel({ detail }: { detail: OperatorAuditDetail }) {
  return (
    <div className="space-y-4 text-sm">
      <div className="grid gap-3 md:grid-cols-2">
        <Info label="Audit Event" value={detail.id} mono />
        <Info label="Outcome" value={detail.outcome} badge />
        <Info label="Actor" value={detail.actorUserId} mono />
        <Info label="Action Type" value={detail.actionType} />
        <Info label="Resource Type" value={detail.resourceType} />
        <Info label="Resource ID" value={detail.resourceId} mono />
        <Info label="Correlation ID" value={detail.correlationId} mono />
        <Info label="Created" value={formatDateTime(detail.createdAt)} />
        <Info label="Request" value={`${detail.requestMethod ?? "—"} ${detail.requestPath ?? "—"}`} />
        <Info label="Reason" value={detail.reason} />
        <Info label="Error Detail" value={detail.errorDetail} />
      </div>
      <div className="space-y-2">
        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Metadata</p>
        <JsonView value={detail.metadata} />
      </div>
    </div>
  );
}

function Info({
  label,
  value,
  mono,
  badge
}: {
  label: string;
  value?: string | number | null;
  mono?: boolean;
  badge?: boolean;
}) {
  return (
    <div className="space-y-1">
      <p className="text-xs uppercase tracking-wide text-muted-foreground">{label}</p>
      {badge ? (
        <Badge className={tone(String(value ?? ""))}>{value ?? "—"}</Badge>
      ) : (
        <p className={mono ? "break-all font-mono text-xs" : ""}>{value ?? "—"}</p>
      )}
    </div>
  );
}
