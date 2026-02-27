"use client";

import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { toast } from "sonner";
import { RoleGate } from "@/components/auth/RoleGate";
import { DataTable } from "@/components/tables/DataTable";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import {
  approveMilestoneMulti,
  approveMilestoneSingle,
  getProject,
  listMilestones,
  releaseRetention,
  requestInspection,
  submitMilestoneEvidence
} from "@/lib/api/endpoints";
import { Milestone } from "@/lib/api/schemas";
import { correlationId } from "@/lib/utils/correlation";
import { formatDateTime } from "@/lib/utils/dates";
import { formatMoney } from "@/lib/utils/money";

const splitFormSchema = z.object({
  supplierUserId: z.string().uuid(),
  supplierAmount: z.coerce.number().positive(),
  inspectorUserId: z.string().uuid(),
  inspectorAmount: z.coerce.number().positive()
});

type SplitForm = z.infer<typeof splitFormSchema>;

export default function ProjectDetailPage({ params }: { params: { id: string } }) {
  const queryClient = useQueryClient();
  const [selectedMilestone, setSelectedMilestone] = useState<string | null>(null);

  const projectQuery = useQuery({ queryKey: ["project", params.id], queryFn: () => getProject(params.id) });
  const milestonesQuery = useQuery({ queryKey: ["project", params.id, "milestones"], queryFn: () => listMilestones(params.id) });

  const splitForm = useForm<SplitForm>({
    resolver: zodResolver(splitFormSchema),
    defaultValues: {
      supplierUserId: "",
      supplierAmount: 0,
      inspectorUserId: "",
      inspectorAmount: 0
    }
  });

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ["project", params.id, "milestones"] });
  };

  const submitEvidence = useMutation({
    mutationFn: (milestoneId: string) => submitMilestoneEvidence(milestoneId, { evidenceUri: "manual://web", comment: "Submitted from dashboard" }),
    onSuccess: refresh,
    onError: () => toast.error("Submit evidence failed")
  });

  const approveSingle = useMutation({
    mutationFn: (milestoneId: string) => approveMilestoneSingle(milestoneId, correlationId()),
    onSuccess: refresh,
    onError: () => toast.error("Approve milestone failed")
  });

  const approveMulti = useMutation({
    mutationFn: ({ milestoneId, values }: { milestoneId: string; values: SplitForm }) =>
      approveMilestoneMulti(milestoneId, {
        idempotencyKey: correlationId(),
        splits: [
          { payeeType: "SUPPLIER", payeeId: values.supplierUserId, amount: values.supplierAmount, businessKey: `SPLIT:${milestoneId}:supplier` },
          { payeeType: "INSPECTOR", payeeId: values.inspectorUserId, amount: values.inspectorAmount, businessKey: `SPLIT:${milestoneId}:inspector` }
        ]
      }),
    onSuccess: async () => {
      splitForm.reset();
      setSelectedMilestone(null);
      await refresh();
    },
    onError: () => toast.error("Approve multi payout failed")
  });

  const retention = useMutation({
    mutationFn: (milestoneId: string) => releaseRetention(milestoneId, correlationId()),
    onSuccess: refresh,
    onError: () => toast.error("Retention release failed")
  });

  const inspection = useMutation({
    mutationFn: (milestoneId: string) => requestInspection({ projectId: params.id, milestoneId, scheduledAt: new Date().toISOString(), feeAmount: 50000 }),
    onSuccess: refresh,
    onError: () => toast.error("Inspection request failed")
  });

  const columns = useMemo<ColumnDef<Milestone>[]>(() => [
    { header: "Seq", accessorKey: "sequenceNo" },
    { header: "Name", accessorKey: "name" },
    { header: "Gross", accessorKey: "amount", cell: ({ row }) => formatMoney(row.original.amount, "TZS") },
    { header: "Retention", accessorKey: "retentionAmount", cell: ({ row }) => formatMoney(row.original.retentionAmount ?? 0, "TZS") },
    { header: "Status", accessorKey: "status", cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { header: "Paid At", accessorKey: "paidAt", cell: ({ row }) => formatDateTime(row.original.paidAt) },
    { header: "Retention Release", accessorKey: "retentionReleaseAt", cell: ({ row }) => formatDateTime(row.original.retentionReleaseAt) },
    {
      header: "Actions",
      cell: ({ row }) => (
        <div className="flex flex-wrap gap-2">
          <RoleGate roles={["CONTRACTOR"]}><Button size="sm" variant="outline" onClick={() => submitEvidence.mutate(row.original.id)}>Submit</Button></RoleGate>
          <RoleGate roles={["OWNER", "ADMIN"]}><Button size="sm" onClick={() => approveSingle.mutate(row.original.id)}>Approve</Button></RoleGate>
          <RoleGate roles={["OWNER", "ADMIN"]}><Button size="sm" variant="outline" onClick={() => inspection.mutate(row.original.id)}>Inspect</Button></RoleGate>
          <RoleGate roles={["OWNER", "ADMIN"]}><Button size="sm" variant="ghost" onClick={() => retention.mutate(row.original.id)}>Release Retention</Button></RoleGate>
          <RoleGate roles={["OWNER", "ADMIN"]}>
            <Dialog>
              <DialogTrigger asChild>
                <Button size="sm" variant="outline" onClick={() => setSelectedMilestone(row.original.id)}>Split Payout</Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader><DialogTitle>Approve Multi-party Splits</DialogTitle></DialogHeader>
                <form className="space-y-3" onSubmit={splitForm.handleSubmit((values) => selectedMilestone && approveMulti.mutate({ milestoneId: selectedMilestone, values }))}>
                  <Input placeholder="Supplier User ID" {...splitForm.register("supplierUserId")} />
                  <Input type="number" step="0.01" placeholder="Supplier Amount" {...splitForm.register("supplierAmount", { valueAsNumber: true })} />
                  <Input placeholder="Inspector User ID" {...splitForm.register("inspectorUserId")} />
                  <Input type="number" step="0.01" placeholder="Inspector Amount" {...splitForm.register("inspectorAmount", { valueAsNumber: true })} />
                  <Button type="submit" disabled={approveMulti.isPending}>Approve Multi</Button>
                </form>
              </DialogContent>
            </Dialog>
          </RoleGate>
        </div>
      )
    }
  ], [submitEvidence, approveSingle, inspection, retention, splitForm, selectedMilestone, approveMulti]);

  if (projectQuery.error) {
    return <p className="text-sm text-red-700">Project detail endpoint unavailable in backend.</p>;
  }

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader><CardTitle>{projectQuery.data?.title ?? "Project"}</CardTitle></CardHeader>
        <CardContent className="space-y-1 text-sm">
          <p>Status: {projectQuery.data?.status}</p>
          <p>Contractor: {projectQuery.data?.contractorUserId ?? "-"}</p>
          <p>Inspector: {projectQuery.data?.inspectorUserId ?? "-"}</p>
        </CardContent>
      </Card>
      {milestonesQuery.error ? (
        <p className="text-sm text-red-700">Milestones endpoint unavailable.</p>
      ) : (
        <DataTable columns={columns} data={milestonesQuery.data ?? []} emptyLabel="No milestones" />
      )}
    </div>
  );
}
