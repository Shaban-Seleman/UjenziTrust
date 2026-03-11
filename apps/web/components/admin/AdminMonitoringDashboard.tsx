"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useQueries } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { RoleGate } from "@/components/auth/RoleGate";
import { DemoQuickStartCard } from "@/components/demo/DemoQuickStartCard";
import { DemoSetupButton } from "@/components/demo/DemoSetupButton";
import { DataTable } from "@/components/tables/DataTable";
import { TablePagination } from "@/components/tables/TablePagination";
import { TableSkeleton } from "@/components/tables/TableSkeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { listLedgerEntriesPage, listOutboxEventsPage, listWebhookEventsPage } from "@/lib/api/endpoints";
import { formatEntityLabel, formatShortId } from "@/lib/utils/labels";

export function AdminMonitoringDashboard() {
  const [outboxPage, setOutboxPage] = useState(0);
  const [webhookPage, setWebhookPage] = useState(0);
  const [ledgerPage, setLedgerPage] = useState(0);
  const pageSize = 8;
  const [outbox, webhooks, ledger] = useQueries({
    queries: [
      { queryKey: ["admin", "outbox", outboxPage, pageSize], queryFn: () => listOutboxEventsPage(outboxPage, pageSize) },
      { queryKey: ["admin", "webhooks", webhookPage, pageSize], queryFn: () => listWebhookEventsPage(webhookPage, pageSize) },
      { queryKey: ["admin", "ledger", ledgerPage, pageSize], queryFn: () => listLedgerEntriesPage(ledgerPage, pageSize) }
    ]
  });

  const outboxCols = useMemo<ColumnDef<Record<string, unknown>>[]>(() => [
    { header: "Event Type", accessorKey: "eventType" },
    { header: "Status", accessorKey: "status" },
    { header: "Idempotency", accessorKey: "idempotencyKey" }
  ], []);

  const webhookCols = useMemo<ColumnDef<Record<string, unknown>>[]>(() => [
    { header: "Event ID", accessorKey: "eventId", cell: ({ row }) => formatEntityLabel("Event", row.original.eventId as string | undefined) },
    { header: "Status", accessorKey: "status" },
    { header: "Received", accessorKey: "receivedAt" }
  ], []);

  const ledgerCols = useMemo<ColumnDef<Record<string, unknown>>[]>(() => [
    { header: "Entry", accessorKey: "entryType" },
    { header: "Reference", accessorKey: "referenceId", cell: ({ row }) => formatEntityLabel("Ref", row.original.referenceId as string | undefined) },
    { header: "Hash", accessorKey: "hash", cell: ({ row }) => formatShortId(row.original.hash as string | undefined) }
  ], []);

  return (
    <RoleGate roles={["ADMIN"]} fallback={<p className="text-sm text-red-700">Not authorized.</p>}>
      <div className="space-y-4">
        <h1 className="text-xl font-semibold">Admin Monitoring</h1>
        <DemoSetupButton />
        <DemoQuickStartCard />
        <Card>
          <CardHeader>
            <CardTitle>Admin Tools</CardTitle>
            <p className="text-sm text-muted-foreground">Operational shortcuts for investor demos and pilot support.</p>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-2">
            <Link className="text-sm text-primary underline-offset-4 hover:underline" href="/admin">Demo Setup</Link>
            <Link className="text-sm text-primary underline-offset-4 hover:underline" href="/admin#outbox">Outbox Events</Link>
            <Link className="text-sm text-primary underline-offset-4 hover:underline" href="/admin#webhooks">Webhook Events</Link>
            <Link className="text-sm text-primary underline-offset-4 hover:underline" href="/app/escrows">Disbursements</Link>
            <Link className="text-sm text-primary underline-offset-4 hover:underline" href="/admin#ledger">Ledger Monitoring</Link>
          </CardContent>
        </Card>
        <Card id="outbox">
          <CardHeader><CardTitle>Outbox Events</CardTitle></CardHeader>
          <CardContent>
            {outbox.isLoading ? (
              <TableSkeleton />
            ) : (
              <div className="space-y-4">
                <DataTable columns={outboxCols} data={(outbox.data?.content as Record<string, unknown>[]) ?? []} emptyLabel={outbox.isError ? "Outbox endpoint unavailable" : "No outbox events"} />
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
        <Card id="webhooks">
          <CardHeader><CardTitle>Webhook Events</CardTitle></CardHeader>
          <CardContent>
            {webhooks.isLoading ? (
              <TableSkeleton />
            ) : (
              <div className="space-y-4">
                <DataTable columns={webhookCols} data={(webhooks.data?.content as Record<string, unknown>[]) ?? []} emptyLabel={webhooks.isError ? "Webhook events endpoint unavailable" : "No webhook events"} />
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
        <Card id="ledger">
          <CardHeader><CardTitle>Ledger Entries</CardTitle></CardHeader>
          <CardContent>
            {ledger.isLoading ? (
              <TableSkeleton />
            ) : (
              <div className="space-y-4">
                <DataTable columns={ledgerCols} data={(ledger.data?.content as Record<string, unknown>[]) ?? []} emptyLabel={ledger.isError ? "Ledger endpoint unavailable" : "No ledger entries"} />
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
      </div>
    </RoleGate>
  );
}
