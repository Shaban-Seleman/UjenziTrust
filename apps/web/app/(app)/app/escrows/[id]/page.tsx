"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { EscrowTimeline, EscrowTimelineSkeleton } from "@/components/escrow/EscrowTimeline";
import { TablePagination } from "@/components/tables/TablePagination";
import { TableSkeleton } from "@/components/tables/TableSkeleton";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { DataTable } from "@/components/tables/DataTable";
import { adaptEscrowTimeline } from "@/lib/adapters/escrow";
import { getEscrow, listDisbursementsByEscrowPage } from "@/lib/api/endpoints";
import { Disbursement } from "@/lib/api/schemas";
import { formatEntityLabel } from "@/lib/utils/labels";
import { formatMoney } from "@/lib/utils/money";

export default function EscrowDetailPage({ params }: { params: { id: string } }) {
  const [page, setPage] = useState(0);
  const pageSize = 8;
  const escrowQuery = useQuery({ queryKey: ["escrow", params.id], queryFn: () => getEscrow(params.id) });
  const disbursementQuery = useQuery({
    queryKey: ["escrow", params.id, "disbursements", page, pageSize],
    queryFn: () => listDisbursementsByEscrowPage(params.id, page, pageSize)
  });

  const columns = useMemo<ColumnDef<Disbursement>[]>(() => [
    { header: "Payee Type", accessorKey: "payeeType" },
    { header: "Payee", accessorKey: "payeeId", cell: ({ row }) => formatEntityLabel(row.original.payeeType ?? "Payee", row.original.payeeId) },
    { header: "Amount", accessorKey: "amount", cell: ({ row }) => formatMoney(row.original.amount, row.original.currency ?? "TZS") },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { header: "Settlement", accessorKey: "settlementRef", cell: ({ row }) => row.original.settlementRef ?? "-" }
  ], []);

  if (escrowQuery.error) {
    return <p className="text-sm text-red-700">Escrow detail endpoint unavailable.</p>;
  }

  const timeline = adaptEscrowTimeline(escrowQuery.data);
  const isLoading = escrowQuery.isLoading;

  return (
    <div className="space-y-4">
      {isLoading ? (
        <EscrowTimelineSkeleton />
      ) : (
        <EscrowTimeline status={timeline.status} timestamps={timeline.timestamps} escrowId={timeline.escrowId} />
      )}
      <Card>
        <CardHeader><CardTitle>{formatEntityLabel("Escrow", escrowQuery.data?.id, "Escrow")}</CardTitle></CardHeader>
        <CardContent className="space-y-1 text-sm">
          {isLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-4 w-40" />
              <Skeleton className="h-4 w-64" />
              <Skeleton className="h-4 w-48" />
            </div>
          ) : (
            <>
              <p>Status: <Badge variant="outline">{escrowQuery.data?.status ?? "-"}</Badge></p>
              <p>Business Key: {escrowQuery.data?.businessKey ?? "\u2014"}</p>
              <p>Total Amount: {escrowQuery.data?.totalAmount !== undefined ? formatMoney(escrowQuery.data.totalAmount, escrowQuery.data?.currency ?? "TZS") : "\u2014"}</p>
            </>
          )}
        </CardContent>
      </Card>
      {disbursementQuery.error ? (
        <p className="text-sm text-red-700">Disbursement endpoint unavailable.</p>
      ) : disbursementQuery.isLoading ? (
        <Card>
          <CardHeader><CardTitle>Disbursements</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            <TableSkeleton />
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          <DataTable columns={columns} data={disbursementQuery.data?.content ?? []} emptyLabel="No disbursements" />
          <TablePagination
            page={page}
            size={pageSize}
            totalElements={disbursementQuery.data?.totalElements ?? 0}
            totalPages={disbursementQuery.data?.totalPages ?? 0}
            onPrevious={() => setPage((value) => Math.max(0, value - 1))}
            onNext={() => setPage((value) => value + 1)}
          />
        </div>
      )}
    </div>
  );
}
