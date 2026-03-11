"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/tables/DataTable";
import { TablePagination } from "@/components/tables/TablePagination";
import { TableSkeleton } from "@/components/tables/TableSkeleton";
import { TableSummaryCards } from "@/components/tables/TableSummaryCards";
import { Badge } from "@/components/ui/badge";
import { listEscrowsPage } from "@/lib/api/endpoints";
import { Escrow } from "@/lib/api/schemas";
import { formatEntityLabel } from "@/lib/utils/labels";
import { formatMoney } from "@/lib/utils/money";

export default function EscrowsPage() {
  const [page, setPage] = useState(0);
  const pageSize = 8;
  const query = useQuery({ queryKey: ["escrows", page, pageSize], queryFn: () => listEscrowsPage(page, pageSize) });

  const columns = useMemo<ColumnDef<Escrow>[]>(() => [
    { header: "Escrow", accessorKey: "id", cell: ({ row }) => <Link className="text-primary" href={`/app/escrows/${row.original.id}`}>{formatEntityLabel("Escrow", row.original.id)}</Link> },
    { header: "Business Key", accessorKey: "businessKey" },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { header: "Total", accessorKey: "totalAmount", cell: ({ row }) => formatMoney(row.original.totalAmount ?? 0, row.original.currency ?? "TZS") }
  ], []);

  if (query.error) {
    return <p className="text-sm text-red-700">Escrow endpoints unavailable in backend.</p>;
  }

  const items = query.data?.content ?? [];
  const activeCount = items.filter((escrow) => escrow.status === "ACTIVE").length;
  const fundedCount = items.filter((escrow) => escrow.status === "FUNDED").length;

  return (
    <div className="space-y-5">
      <div className="space-y-1">
        <h1 className="text-xl font-semibold">Escrows</h1>
        <p className="text-sm text-muted-foreground">Track visible escrow records, funding state, and settlement progress.</p>
      </div>
      <TableSummaryCards
        items={[
          { label: "Visible Escrows", value: query.data?.totalElements ?? 0, hint: "Across all pages" },
          { label: "Active", value: activeCount, hint: "Visible on this page" },
          { label: "Funded", value: fundedCount, hint: "Visible on this page" }
        ]}
      />
      {query.isLoading ? (
        <TableSkeleton />
      ) : (
        <>
          <DataTable columns={columns} data={query.data?.content ?? []} emptyLabel="No escrows found" />
          <TablePagination
            page={page}
            size={pageSize}
            totalElements={query.data?.totalElements ?? 0}
            totalPages={query.data?.totalPages ?? 0}
            onPrevious={() => setPage((value) => Math.max(0, value - 1))}
            onNext={() => setPage((value) => value + 1)}
          />
        </>
      )}
    </div>
  );
}
