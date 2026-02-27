"use client";

import { useMemo } from "react";
import { useQueries } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { RoleGate } from "@/components/auth/RoleGate";
import { DataTable } from "@/components/tables/DataTable";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { listLedgerEntries, listOutboxEvents, listWebhookEvents } from "@/lib/api/endpoints";

export default function AdminPage() {
  const [outbox, webhooks, ledger] = useQueries({
    queries: [
      { queryKey: ["admin", "outbox"], queryFn: listOutboxEvents },
      { queryKey: ["admin", "webhooks"], queryFn: listWebhookEvents },
      { queryKey: ["admin", "ledger"], queryFn: listLedgerEntries }
    ]
  });

  const outboxCols = useMemo<ColumnDef<Record<string, unknown>>[]>(() => [
    { header: "Event Type", accessorKey: "eventType" },
    { header: "Status", accessorKey: "status" },
    { header: "Idempotency", accessorKey: "idempotencyKey" }
  ], []);

  const webhookCols = useMemo<ColumnDef<Record<string, unknown>>[]>(() => [
    { header: "Event ID", accessorKey: "eventId" },
    { header: "Status", accessorKey: "status" },
    { header: "Received", accessorKey: "receivedAt" }
  ], []);

  const ledgerCols = useMemo<ColumnDef<Record<string, unknown>>[]>(() => [
    { header: "Entry", accessorKey: "entryType" },
    { header: "Reference", accessorKey: "referenceId" },
    { header: "Hash", accessorKey: "hash" }
  ], []);

  return (
    <RoleGate roles={["ADMIN"]} fallback={<p className="text-sm text-red-700">Not authorized.</p>}>
      <div className="space-y-4">
        <h1 className="text-xl font-semibold">Admin Monitoring</h1>
        <Card>
          <CardHeader><CardTitle>Outbox Events</CardTitle></CardHeader>
          <CardContent>
            <DataTable columns={outboxCols} data={(outbox.data as Record<string, unknown>[]) ?? []} emptyLabel="Outbox endpoint unavailable" />
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Webhook Events</CardTitle></CardHeader>
          <CardContent>
            <DataTable columns={webhookCols} data={(webhooks.data as Record<string, unknown>[]) ?? []} emptyLabel="Webhook events endpoint unavailable" />
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Ledger Entries</CardTitle></CardHeader>
          <CardContent>
            <DataTable columns={ledgerCols} data={(ledger.data as Record<string, unknown>[]) ?? []} emptyLabel="Ledger endpoint unavailable" />
          </CardContent>
        </Card>
      </div>
    </RoleGate>
  );
}
