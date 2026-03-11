"use client";

import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
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
import { Input } from "@/components/ui/input";
import { acceptOffer, counterOffer, listOffersPage, rejectOffer, withdrawOffer } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/errors";
import { Offer } from "@/lib/api/schemas";
import { correlationId } from "@/lib/utils/correlation";
import { formatEntityLabel } from "@/lib/utils/labels";
import { formatMoney } from "@/lib/utils/money";

const counterSchema = z.object({
  amount: z.coerce.number().positive(),
  notes: z.string().max(500).optional()
});

type CounterForm = z.infer<typeof counterSchema>;

export default function OffersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const pageSize = 8;
  const query = useQuery({ queryKey: ["offers", page, pageSize], queryFn: () => listOffersPage(page, pageSize) });
  const counterForm = useForm<CounterForm>({
    resolver: zodResolver(counterSchema),
    defaultValues: { amount: 0, notes: "" }
  });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ["offers"] });
  const showError = (error: unknown, fallback: string) => {
    const message = error instanceof ApiError ? error.detail : fallback;
    toast.error(message);
  };

  const accept = useMutation({
    mutationFn: (id: string) => acceptOffer(id, { idempotencyKey: correlationId() }),
    onSuccess: async () => {
      toast.success("Offer accepted");
      await refresh();
    },
    onError: (error) => showError(error, "Accept failed")
  });
  const reject = useMutation({
    mutationFn: (id: string) => rejectOffer(id),
    onSuccess: async () => {
      toast.success("Offer rejected");
      await refresh();
    },
    onError: (error) => showError(error, "Reject failed")
  });
  const withdraw = useMutation({
    mutationFn: (id: string) => withdrawOffer(id),
    onSuccess: async () => {
      toast.success("Offer withdrawn");
      await refresh();
    },
    onError: (error) => showError(error, "Withdraw failed")
  });
  const counter = useMutation({
    mutationFn: ({ id, values }: { id: string; values: CounterForm }) => counterOffer(id, values),
    onSuccess: async () => {
      toast.success("Counter offer submitted");
      counterForm.reset({ amount: 0, notes: "" });
      await refresh();
    },
    onError: (error) => showError(error, "Counter failed")
  });

  const columns = useMemo<ColumnDef<Offer>[]>(() => [
    { header: "Amount", accessorKey: "amount", cell: ({ row }) => formatMoney(row.original.amount, row.original.currency ?? "TZS") },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { header: "Property", accessorKey: "propertyId", cell: ({ row }) => formatEntityLabel("Property", row.original.propertyId) },
    {
      header: "Actions",
      cell: ({ row }) => (
        <div className="flex gap-2">
          {(() => {
            const status = row.original.status;
            const canCounter = status === "SUBMITTED" || status === "COUNTERED";
            const canAcceptOrReject = status === "SUBMITTED" || status === "COUNTERED";
            const canWithdraw = status === "SUBMITTED" || status === "COUNTERED";
            return (
              <>
          <RoleGate roles={["SELLER", "OWNER", "ADMIN"]}>
            <Dialog>
              <DialogTrigger asChild>
                <Button size="sm" variant="outline" disabled={!canCounter || counter.isPending}>Counter</Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader><DialogTitle>Counter Offer</DialogTitle></DialogHeader>
                <form
                  className="space-y-3"
                    onSubmit={counterForm.handleSubmit((values) => counter.mutate({ id: row.original.id, values }))}
                >
                  <Input type="number" step="0.01" placeholder="Counter amount" {...counterForm.register("amount", { valueAsNumber: true })} />
                  <Input placeholder="Notes (optional)" {...counterForm.register("notes")} />
                  <Button type="submit" disabled={!canCounter || counter.isPending}>
                    {counter.isPending ? "Submitting..." : "Submit Counter"}
                  </Button>
                </form>
              </DialogContent>
            </Dialog>
            <Button size="sm" onClick={() => accept.mutate(row.original.id)} disabled={!canAcceptOrReject || accept.isPending}>Accept</Button>
            <Button size="sm" variant="outline" onClick={() => reject.mutate(row.original.id)} disabled={!canAcceptOrReject || reject.isPending}>Reject</Button>
          </RoleGate>
          <RoleGate roles={["BUYER"]}>
            <Button size="sm" variant="ghost" onClick={() => withdraw.mutate(row.original.id)} disabled={!canWithdraw || withdraw.isPending}>Withdraw</Button>
          </RoleGate>
              </>
            );
          })()}
        </div>
      )
    }
  ], [accept, counter, counterForm, reject, withdraw]);

  if (query.error) {
    return <p className="text-sm text-red-700">Offers list endpoint unavailable in backend.</p>;
  }

  const items = query.data?.content ?? [];
  const submittedCount = items.filter((offer) => offer.status === "SUBMITTED").length;
  const counteredCount = items.filter((offer) => offer.status === "COUNTERED").length;

  return (
    <div className="space-y-5">
      <div className="space-y-1">
        <h1 className="text-xl font-semibold">Offers</h1>
        <p className="text-sm text-muted-foreground">Review current negotiations and act on buyer or seller responses.</p>
      </div>
      <TableSummaryCards
        items={[
          { label: "Visible Offers", value: query.data?.totalElements ?? 0, hint: "Across all pages" },
          { label: "Submitted", value: submittedCount, hint: "Visible on this page" },
          { label: "Countered", value: counteredCount, hint: "Visible on this page" }
        ]}
      />
      {query.isLoading ? (
        <TableSkeleton />
      ) : (
        <>
          <DataTable columns={columns} data={query.data?.content ?? []} emptyLabel="No offers available" />
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
