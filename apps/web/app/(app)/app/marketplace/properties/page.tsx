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
import { Button } from "@/components/ui/button";
import { authMe, listMyPropertiesPage, listPropertiesPage } from "@/lib/api/endpoints";
import { Property } from "@/lib/api/schemas";
import { formatMoney } from "@/lib/utils/money";

export default function PropertiesPage() {
  const [page, setPage] = useState(0);
  const pageSize = 8;
  const actorQuery = useQuery({ queryKey: ["auth", "me"], queryFn: authMe });
  const isOwnerView = actorQuery.data?.roles?.some((role) => role === "OWNER" || role === "SELLER") ?? false;
  const query = useQuery({
    queryKey: ["properties", isOwnerView ? "mine" : "published", page, pageSize],
    queryFn: () => (isOwnerView ? listMyPropertiesPage(page, pageSize) : listPropertiesPage(page, pageSize)),
    enabled: actorQuery.isSuccess
  });

  const columns = useMemo<ColumnDef<Property>[]>(() => [
    { header: "Title", accessorKey: "title", cell: ({ row }) => <Link className="text-primary" href={`/app/marketplace/properties/${row.original.id}`}>{row.original.title}</Link> },
    { header: "Location", accessorKey: "location", cell: ({ row }) => row.original.location ?? "-" },
    { header: "Price", accessorKey: "askingPrice", cell: ({ row }) => formatMoney(row.original.askingPrice ?? 0, row.original.currency ?? "TZS") },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> }
  ], []);

  const items = query.data?.content ?? [];
  const draftCount = items.filter((property) => property.status === "DRAFT").length;
  const publishedCount = items.filter((property) => property.status === "PUBLISHED").length;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div className="space-y-1">
          <h1 className="text-xl font-semibold">Properties</h1>
          <p className="text-sm text-muted-foreground">
            {isOwnerView ? "Manage your listings, drafts, and published inventory." : "Browse available marketplace listings."}
          </p>
        </div>
        <Link href="/app/marketplace/properties/new"><Button>Create Property</Button></Link>
      </div>

      <TableSummaryCards
        items={[
          {
            label: "Showing",
            value: query.data?.totalElements ?? 0,
            hint: isOwnerView ? "Your total listings" : "Published listings"
          },
          {
            label: "Drafts",
            value: draftCount,
            hint: "Visible on this page"
          },
          {
            label: "Published",
            value: publishedCount,
            hint: "Visible on this page"
          }
        ]}
      />

      {actorQuery.isLoading || query.isLoading ? (
        <TableSkeleton rows={4} />
      ) : null}

      {query.error ? (
        <p className="text-sm text-red-700">Properties endpoint unavailable or failed.</p>
      ) : query.isLoading ? null : (
        <>
          <DataTable columns={columns} data={items} emptyLabel="No properties found" />
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
