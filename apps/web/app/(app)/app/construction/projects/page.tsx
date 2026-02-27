"use client";

import Link from "next/link";
import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Badge } from "@/components/ui/badge";
import { DataTable } from "@/components/tables/DataTable";
import { listProjects } from "@/lib/api/endpoints";
import { Project } from "@/lib/api/schemas";

export default function ProjectsPage() {
  const query = useQuery({ queryKey: ["projects"], queryFn: listProjects });

  const columns = useMemo<ColumnDef<Project>[]>(() => [
    { header: "Project", accessorKey: "title", cell: ({ row }) => <Link className="text-primary" href={`/app/construction/projects/${row.original.id}`}>{row.original.title}</Link> },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { header: "Contractor", accessorKey: "contractorUserId", cell: ({ row }) => row.original.contractorUserId ?? "-" },
    { header: "Inspector", accessorKey: "inspectorUserId", cell: ({ row }) => row.original.inspectorUserId ?? "-" }
  ], []);

  if (query.error) {
    return <p className="text-sm text-red-700">Projects list endpoint unavailable in backend.</p>;
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Construction Projects</h1>
      <DataTable columns={columns} data={query.data ?? []} emptyLabel="No projects found" />
    </div>
  );
}
