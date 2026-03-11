"use client";

import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { toast } from "sonner";
import { DataTable } from "@/components/tables/DataTable";
import { TablePagination } from "@/components/tables/TablePagination";
import { TableSkeleton } from "@/components/tables/TableSkeleton";
import { TableSummaryCards } from "@/components/tables/TableSummaryCards";
import { RoleGate } from "@/components/auth/RoleGate";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { cancelReservation, listReservationsPage } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/errors";
import { Reservation } from "@/lib/api/schemas";
import { formatEntityLabel } from "@/lib/utils/labels";

export default function ReservationsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const pageSize = 8;
  const query = useQuery({ queryKey: ["reservations", page, pageSize], queryFn: () => listReservationsPage(page, pageSize) });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["reservations"] });

  const cancel = useMutation({
    mutationFn: (reservationId: string) => cancelReservation(reservationId, "Cancelled from dashboard"),
    onSuccess: async () => {
      toast.success("Reservation cancelled");
      await refresh();
    },
    onError: (error: unknown) => {
      const message = error instanceof ApiError ? error.detail : "Cancel failed";
      toast.error(message);
    }
  });

  const columns = useMemo<ColumnDef<Reservation>[]>(() => [
    { header: "Reservation", accessorKey: "id", cell: ({ row }) => formatEntityLabel("Reservation", row.original.id) },
    { header: "Property", accessorKey: "propertyId", cell: ({ row }) => formatEntityLabel("Property", row.original.propertyId) },
    { header: "Offer", accessorKey: "offerId", cell: ({ row }) => formatEntityLabel("Offer", row.original.offerId) },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { header: "Reserved Until", accessorKey: "reservedUntil", cell: ({ row }) => row.original.reservedUntil ?? "-" },
    { header: "Escrow", accessorKey: "escrowId", cell: ({ row }) => formatEntityLabel("Escrow", row.original.escrowId) },
    {
      header: "Actions",
      cell: ({ row }) => {
        const cancellable = row.original.status === "ACTIVE";
        return (
          <RoleGate roles={["SELLER", "OWNER"]}>
            <Dialog>
              <DialogTrigger asChild>
                <Button size="sm" variant="outline" disabled={!cancellable || cancel.isPending}>
                  Cancel
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader><DialogTitle>Cancel Reservation</DialogTitle></DialogHeader>
                <p className="text-sm text-muted-foreground">
                  This will cancel the active reservation and update the linked offer status where applicable.
                </p>
                <Button
                  onClick={() => cancel.mutate(row.original.id)}
                  disabled={!cancellable || cancel.isPending}
                >
                  {cancel.isPending ? "Cancelling..." : "Confirm Cancel"}
                </Button>
              </DialogContent>
            </Dialog>
          </RoleGate>
        );
      }
    }
  ], [cancel]);

  if (query.error) {
    return <p className="text-sm text-red-700">Reservations list endpoint unavailable in backend.</p>;
  }

  const items = query.data?.content ?? [];
  const activeCount = items.filter((reservation) => reservation.status === "ACTIVE").length;
  const escrowLinkedCount = items.filter((reservation) => Boolean(reservation.escrowId)).length;

  return (
    <div className="space-y-5">
      <div className="space-y-1">
        <h1 className="text-xl font-semibold">Reservations</h1>
        <p className="text-sm text-muted-foreground">Monitor accepted offers, hold windows, and linked escrow activity.</p>
      </div>
      <TableSummaryCards
        items={[
          { label: "Visible Reservations", value: query.data?.totalElements ?? 0, hint: "Across all pages" },
          { label: "Active", value: activeCount, hint: "Visible on this page" },
          { label: "Escrow Linked", value: escrowLinkedCount, hint: "Visible on this page" }
        ]}
      />
      {query.isLoading ? (
        <TableSkeleton />
      ) : (
        <>
          <DataTable columns={columns} data={query.data?.content ?? []} emptyLabel="No reservations found" />
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
