"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { RoleGate } from "@/components/auth/RoleGate";
import { DataTable } from "@/components/tables/DataTable";
import { TablePagination } from "@/components/tables/TablePagination";
import { TableSkeleton } from "@/components/tables/TableSkeleton";
import { TableSummaryCards } from "@/components/tables/TableSummaryCards";
import { listProjectsPage } from "@/lib/api/endpoints";
import { Project } from "@/lib/api/schemas";
import { formatUserLabel } from "@/lib/utils/labels";

export default function ProjectsPage() {
  const [page, setPage] = useState(0);
  const pageSize = 8;
  const query = useQuery({ queryKey: ["projects", page, pageSize], queryFn: () => listProjectsPage(page, pageSize) });

  const columns = useMemo<ColumnDef<Project>[]>(() => [
    { header: "Project", accessorKey: "title", cell: ({ row }) => <Link className="text-primary" href={`/app/construction/projects/${row.original.id}`}>{row.original.title}</Link> },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { header: "Contractor", accessorKey: "contractorUserId", cell: ({ row }) => formatUserLabel("Contractor", row.original.contractorUserId) },
    { header: "Inspector", accessorKey: "inspectorUserId", cell: ({ row }) => formatUserLabel("Inspector", row.original.inspectorUserId) }
  ], []);

  if (query.error) {
    return <p className="text-sm text-red-700">Projects list endpoint unavailable in backend.</p>;
  }

  const items = query.data?.content ?? [];
  const draftCount = items.filter((project) => project.status === "DRAFT").length;
  const activeCount = items.filter((project) => project.status === "ACTIVE").length;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div className="space-y-1">
          <h1 className="text-xl font-semibold">Construction Projects</h1>
          <p className="text-sm text-muted-foreground">Manage visible projects, assignments, and execution status.</p>
        </div>
        <RoleGate roles={["OWNER"]}>
          <Link href="/app/construction/projects/new">
            <Button>Create Project</Button>
          </Link>
        </RoleGate>
      </div>
      <TableSummaryCards
        items={[
          { label: "Visible Projects", value: query.data?.totalElements ?? 0, hint: "Across all pages" },
          { label: "Draft", value: draftCount, hint: "Visible on this page" },
          { label: "Active", value: activeCount, hint: "Visible on this page" }
        ]}
      />
      {query.isLoading ? (
        <TableSkeleton />
      ) : (
        <>
          <DataTable columns={columns} data={query.data?.content ?? []} emptyLabel="No projects found" />
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
