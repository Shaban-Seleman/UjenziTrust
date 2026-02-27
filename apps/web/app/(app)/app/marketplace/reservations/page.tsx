"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/tables/DataTable";
import { Badge } from "@/components/ui/badge";
import { listReservations } from "@/lib/api/endpoints";
import { Reservation } from "@/lib/api/schemas";

export default function ReservationsPage() {
  const query = useQuery({ queryKey: ["reservations"], queryFn: listReservations });

  const columns = useMemo<ColumnDef<Reservation>[]>(() => [
    { header: "Reservation", accessorKey: "id" },
    { header: "Property", accessorKey: "propertyId" },
    { header: "Offer", accessorKey: "offerId" },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { header: "Reserved Until", accessorKey: "reservedUntil", cell: ({ row }) => row.original.reservedUntil ?? "-" },
    { header: "Escrow", accessorKey: "escrowId", cell: ({ row }) => row.original.escrowId ?? "-" }
  ], []);

  if (query.error) {
    return <p className="text-sm text-red-700">Reservations list endpoint unavailable in backend.</p>;
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Reservations</h1>
      <DataTable columns={columns} data={query.data ?? []} emptyLabel="No active reservations" />
    </div>
  );
}
