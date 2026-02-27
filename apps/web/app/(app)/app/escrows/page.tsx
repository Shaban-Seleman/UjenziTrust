"use client";

import Link from "next/link";
import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/tables/DataTable";
import { Badge } from "@/components/ui/badge";
import { listEscrows } from "@/lib/api/endpoints";
import { Escrow } from "@/lib/api/schemas";
import { formatMoney } from "@/lib/utils/money";

export default function EscrowsPage() {
  const query = useQuery({ queryKey: ["escrows"], queryFn: listEscrows });

  const columns = useMemo<ColumnDef<Escrow>[]>(() => [
    { header: "Escrow", accessorKey: "id", cell: ({ row }) => <Link className="text-primary" href={`/app/escrows/${row.original.id}`}>{row.original.id}</Link> },
    { header: "Business Key", accessorKey: "businessKey" },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { header: "Total", accessorKey: "totalAmount", cell: ({ row }) => formatMoney(row.original.totalAmount ?? 0, row.original.currency ?? "TZS") }
  ], []);

  if (query.error) {
    return <p className="text-sm text-red-700">Escrow endpoints unavailable in backend.</p>;
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Escrows</h1>
      <DataTable columns={columns} data={query.data ?? []} emptyLabel="No escrows found" />
    </div>
  );
}
