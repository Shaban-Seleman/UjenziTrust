"use client";

import { useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { toast } from "sonner";
import { DataTable } from "@/components/tables/DataTable";
import { RoleGate } from "@/components/auth/RoleGate";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { acceptOffer, listOffers, rejectOffer, withdrawOffer } from "@/lib/api/endpoints";
import { Offer } from "@/lib/api/schemas";
import { correlationId } from "@/lib/utils/correlation";
import { formatMoney } from "@/lib/utils/money";

export default function OffersPage() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ["offers"], queryFn: listOffers });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ["offers"] });

  const accept = useMutation({ mutationFn: (id: string) => acceptOffer(id, { idempotencyKey: correlationId() }), onSuccess: refresh, onError: () => toast.error("Accept failed") });
  const reject = useMutation({ mutationFn: (id: string) => rejectOffer(id), onSuccess: refresh, onError: () => toast.error("Reject failed") });
  const withdraw = useMutation({ mutationFn: (id: string) => withdrawOffer(id), onSuccess: refresh, onError: () => toast.error("Withdraw failed") });

  const columns = useMemo<ColumnDef<Offer>[]>(() => [
    { header: "Amount", accessorKey: "amount", cell: ({ row }) => formatMoney(row.original.amount, row.original.currency ?? "TZS") },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { header: "Property", accessorKey: "propertyId" },
    {
      header: "Actions",
      cell: ({ row }) => (
        <div className="flex gap-2">
          <RoleGate roles={["SELLER", "OWNER", "ADMIN"]}>
            <Button size="sm" onClick={() => accept.mutate(row.original.id)}>Accept</Button>
            <Button size="sm" variant="outline" onClick={() => reject.mutate(row.original.id)}>Reject</Button>
          </RoleGate>
          <RoleGate roles={["BUYER"]}>
            <Button size="sm" variant="ghost" onClick={() => withdraw.mutate(row.original.id)}>Withdraw</Button>
          </RoleGate>
        </div>
      )
    }
  ], [accept, reject, withdraw]);

  if (query.error) {
    return <p className="text-sm text-red-700">Offers list endpoint unavailable in backend.</p>;
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Offers</h1>
      <DataTable columns={columns} data={query.data ?? []} emptyLabel="No offers available" />
    </div>
  );
}
