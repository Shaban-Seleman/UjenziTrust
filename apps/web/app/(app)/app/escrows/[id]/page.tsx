"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DataTable } from "@/components/tables/DataTable";
import { getEscrow, listDisbursementsByEscrow } from "@/lib/api/endpoints";
import { Disbursement } from "@/lib/api/schemas";
import { formatMoney } from "@/lib/utils/money";

export default function EscrowDetailPage({ params }: { params: { id: string } }) {
  const escrowQuery = useQuery({ queryKey: ["escrow", params.id], queryFn: () => getEscrow(params.id) });
  const disbursementQuery = useQuery({ queryKey: ["escrow", params.id, "disbursements"], queryFn: () => listDisbursementsByEscrow(params.id) });

  const columns = useMemo<ColumnDef<Disbursement>[]>(() => [
    { header: "Payee Type", accessorKey: "payeeType" },
    { header: "Payee", accessorKey: "payeeId" },
    { header: "Amount", accessorKey: "amount", cell: ({ row }) => formatMoney(row.original.amount, row.original.currency ?? "TZS") },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { header: "Settlement", accessorKey: "settlementRef", cell: ({ row }) => row.original.settlementRef ?? "-" }
  ], []);

  if (escrowQuery.error) {
    return <p className="text-sm text-red-700">Escrow detail endpoint unavailable.</p>;
  }

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader><CardTitle>Escrow {escrowQuery.data?.id}</CardTitle></CardHeader>
        <CardContent className="space-y-1 text-sm">
          <p>Status: <Badge variant="outline">{escrowQuery.data?.status ?? "-"}</Badge></p>
          <p>Business Key: {escrowQuery.data?.businessKey}</p>
          <p>Total Amount: {formatMoney(escrowQuery.data?.totalAmount ?? 0, escrowQuery.data?.currency ?? "TZS")}</p>
        </CardContent>
      </Card>
      {disbursementQuery.error ? (
        <p className="text-sm text-red-700">Disbursement endpoint unavailable.</p>
      ) : (
        <DataTable columns={columns} data={disbursementQuery.data ?? []} emptyLabel="No disbursements" />
      )}
    </div>
  );
}
