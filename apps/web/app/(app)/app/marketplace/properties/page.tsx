"use client";

import Link from "next/link";
import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/tables/DataTable";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { listProperties } from "@/lib/api/endpoints";
import { Property } from "@/lib/api/schemas";
import { formatMoney } from "@/lib/utils/money";

export default function PropertiesPage() {
  const query = useQuery({ queryKey: ["properties"], queryFn: listProperties });

  const columns = useMemo<ColumnDef<Property>[]>(() => [
    { header: "Title", accessorKey: "title", cell: ({ row }) => <Link className="text-primary" href={`/app/marketplace/properties/${row.original.id}`}>{row.original.title}</Link> },
    { header: "Location", accessorKey: "location", cell: ({ row }) => row.original.location ?? "-" },
    { header: "Price", accessorKey: "askingPrice", cell: ({ row }) => formatMoney(row.original.askingPrice ?? 0, row.original.currency ?? "TZS") },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> }
  ], []);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Properties</h1>
        <Link href="/app/marketplace/properties/new"><Button>Create Property</Button></Link>
      </div>
      {query.error ? (
        <p className="text-sm text-red-700">Properties endpoint unavailable or failed.</p>
      ) : (
        <DataTable columns={columns} data={query.data ?? []} emptyLabel="No properties found" />
      )}
    </div>
  );
}
