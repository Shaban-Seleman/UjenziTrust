"use client";

import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { toast } from "sonner";
import { RoleGate } from "@/components/auth/RoleGate";
import { MilestoneTimeline, MilestoneTimelineSkeleton } from "@/components/construction/MilestoneTimeline";
import { DataTable } from "@/components/tables/DataTable";
import { TablePagination } from "@/components/tables/TablePagination";
import { TableSkeleton } from "@/components/tables/TableSkeleton";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { adaptMilestones } from "@/lib/adapters/milestone";
import {
  activateProject,
  approveMilestoneMulti,
  approveMilestoneSingle,
  assignProjectParticipants,
  createMilestone,
  getProject,
  getMilestone,
  listDirectoryUsers,
  listMilestonesPage,
  releaseRetention,
  requestInspection,
  submitMilestoneEvidence
} from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/errors";
import { DirectoryUser, Milestone } from "@/lib/api/schemas";
import { correlationId } from "@/lib/utils/correlation";
import { formatDateTime } from "@/lib/utils/dates";
import { formatUserLabel } from "@/lib/utils/labels";
import { formatMoney } from "@/lib/utils/money";

const splitFormSchema = z.object({
  supplierUserId: z.string().uuid("Supplier user ID must be a valid UUID"),
  supplierAmount: z.coerce.number().positive("Supplier amount must be positive"),
  inspectorUserId: z.string().uuid("Inspector user ID must be a valid UUID"),
  inspectorAmount: z.coerce.number().positive("Inspector amount must be positive")
});

const assignmentSchema = z.object({
  contractorUserId: z.string().uuid("Contractor user ID must be a valid UUID"),
  inspectorUserId: z.string().uuid("Inspector user ID must be a valid UUID")
});

const milestoneCreateSchema = z.object({
  sequenceNo: z.coerce.number().int().positive("Sequence number is required"),
  name: z.string().min(2, "Name is required"),
  description: z.string().optional(),
  amount: z.coerce.number().positive("Gross amount must be positive"),
  retentionAmount: z.preprocess(
    (value) => (value === "" || value === null || value === undefined || Number.isNaN(value) ? undefined : value),
    z.coerce.number().min(0).optional()
  ),
  dueDate: z.string().optional()
});

const evidenceSchema = z.object({
  evidenceUri: z.string().min(3, "Evidence URI or reference is required"),
  notes: z.string().min(2, "Notes are required")
});

type SplitForm = z.infer<typeof splitFormSchema>;
type AssignmentForm = z.infer<typeof assignmentSchema>;
type CreateMilestoneForm = z.infer<typeof milestoneCreateSchema>;
type EvidenceForm = z.infer<typeof evidenceSchema>;
function errorDetail(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.detail : fallback;
}

function directoryLabel(user: DirectoryUser) {
  return user.email ?? user.phone ?? user.id;
}

export default function ProjectDetailPage({ params }: { params: { id: string } }) {
  const queryClient = useQueryClient();
  const [milestonesPage, setMilestonesPage] = useState(0);
  const milestonePageSize = 8;
  const [selectedSplitMilestone, setSelectedSplitMilestone] = useState<string | null>(null);
  const [selectedEvidenceMilestone, setSelectedEvidenceMilestone] = useState<string | null>(null);
  const [showAssign, setShowAssign] = useState(false);
  const [showCreateMilestone, setShowCreateMilestone] = useState(false);
  const [showActivateConfirm, setShowActivateConfirm] = useState(false);

  const projectQuery = useQuery({ queryKey: ["project", params.id], queryFn: () => getProject(params.id) });
  const milestonesQuery = useQuery({
    queryKey: ["project", params.id, "milestones", milestonesPage, milestonePageSize],
    queryFn: () => listMilestonesPage(params.id, milestonesPage, milestonePageSize)
  });
  const contractorsQuery = useQuery({
    queryKey: ["directory", "CONTRACTOR"],
    queryFn: () => listDirectoryUsers("CONTRACTOR"),
    enabled: showAssign
  });
  const inspectorsQuery = useQuery({
    queryKey: ["directory", "INSPECTOR"],
    queryFn: () => listDirectoryUsers("INSPECTOR"),
    enabled: showAssign
  });
  const evidenceMilestoneQuery = useQuery({
    queryKey: ["milestone", selectedEvidenceMilestone],
    queryFn: () => getMilestone(selectedEvidenceMilestone!),
    enabled: !!selectedEvidenceMilestone
  });
  const splitMilestoneQuery = useQuery({
    queryKey: ["milestone", selectedSplitMilestone],
    queryFn: () => getMilestone(selectedSplitMilestone!),
    enabled: !!selectedSplitMilestone
  });

  const splitForm = useForm<SplitForm>({
    resolver: zodResolver(splitFormSchema),
    defaultValues: {
      supplierUserId: "",
      supplierAmount: 0,
      inspectorUserId: "",
      inspectorAmount: 0
    }
  });

  const assignmentForm = useForm<AssignmentForm>({
    resolver: zodResolver(assignmentSchema),
    defaultValues: {
      contractorUserId: "",
      inspectorUserId: ""
    }
  });

  const milestoneCreateForm = useForm<CreateMilestoneForm>({
    resolver: zodResolver(milestoneCreateSchema),
    defaultValues: {
      sequenceNo: 1,
      name: "",
      description: "",
      amount: 0,
      retentionAmount: undefined,
      dueDate: ""
    }
  });

  const evidenceForm = useForm<EvidenceForm>({
    resolver: zodResolver(evidenceSchema),
    defaultValues: {
      evidenceUri: "",
      notes: ""
    }
  });

  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["project", params.id] }),
      queryClient.invalidateQueries({ queryKey: ["project", params.id, "milestones"] })
    ]);
  };

  const refreshMilestone = async (milestoneId: string | null) => {
    if (!milestoneId) {
      return;
    }
    await queryClient.invalidateQueries({ queryKey: ["milestone", milestoneId] });
  };

  const submitEvidence = useMutation({
    mutationFn: ({ milestoneId, payload }: { milestoneId: string; payload: EvidenceForm }) =>
      submitMilestoneEvidence(milestoneId, {
        evidence: {
          evidenceUri: payload.evidenceUri,
          source: "web-dashboard",
          submittedAt: new Date().toISOString()
        },
        notes: payload.notes
      }),
    onSuccess: async () => {
      toast.success("Evidence submitted");
      evidenceForm.reset({ evidenceUri: "", notes: "" });
      const milestoneId = selectedEvidenceMilestone;
      setSelectedEvidenceMilestone(null);
      await refreshMilestone(milestoneId);
      await refresh();
    },
    onError: (error) => toast.error(errorDetail(error, "Submit evidence failed"))
  });

  const assignParticipantsMutation = useMutation({
    mutationFn: (data: AssignmentForm) => assignProjectParticipants(params.id, data),
    onSuccess: async () => {
      toast.success("Participants assigned");
      setShowAssign(false);
      await refresh();
    },
    onError: (error) => toast.error(errorDetail(error, "Assign participants failed"))
  });

  const activateMutation = useMutation({
    mutationFn: () => activateProject(params.id),
    onSuccess: async () => {
      toast.success("Project activated");
      setShowActivateConfirm(false);
      await refresh();
    },
    onError: (error) => toast.error(errorDetail(error, "Activate project failed"))
  });

  const createMilestoneMutation = useMutation({
    mutationFn: (values: CreateMilestoneForm) =>
      createMilestone(params.id, {
        sequenceNo: values.sequenceNo,
        name: values.name,
        description: values.description,
        amount: values.amount,
        retentionAmount: Number.isNaN(values.retentionAmount) ? undefined : values.retentionAmount,
        dueDate: values.dueDate || undefined
      }),
    onSuccess: async () => {
      toast.success("Milestone created");
      setShowCreateMilestone(false);
      milestoneCreateForm.reset({
        sequenceNo: 1,
        name: "",
        description: "",
        amount: 0,
        retentionAmount: undefined,
        dueDate: ""
      });
      await refresh();
    },
    onError: (error) => toast.error(errorDetail(error, "Create milestone failed"))
  });

  const approveSingle = useMutation({
    mutationFn: (milestoneId: string) => approveMilestoneSingle(milestoneId, correlationId()),
    onSuccess: async (_data, milestoneId) => {
      toast.success("Milestone approved");
      await refreshMilestone(milestoneId);
      await refresh();
    },
    onError: (error) => toast.error(errorDetail(error, "Approve milestone failed"))
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
      toast.success("Split payout approved");
      splitForm.reset();
      const milestoneId = selectedSplitMilestone;
      setSelectedSplitMilestone(null);
      await refreshMilestone(milestoneId);
      await refresh();
    },
    onError: (error) => toast.error(errorDetail(error, "Approve multi payout failed"))
  });

  const retention = useMutation({
    mutationFn: (milestoneId: string) => releaseRetention(milestoneId, correlationId()),
    onSuccess: async (_data, milestoneId) => {
      toast.success("Retention released");
      await refreshMilestone(milestoneId);
      await refresh();
    },
    onError: (error) => toast.error(errorDetail(error, "Retention release failed"))
  });

  const inspection = useMutation({
    mutationFn: (milestoneId: string) =>
      requestInspection({ projectId: params.id, milestoneId, scheduledAt: new Date().toISOString(), feeAmount: 50000 }),
    onSuccess: async (_data, milestoneId) => {
      toast.success("Inspection requested");
      await refreshMilestone(milestoneId);
      await refresh();
    },
    onError: (error) => toast.error(errorDetail(error, "Inspection request failed"))
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
          <RoleGate roles={["CONTRACTOR"]}>
            <Dialog>
              <DialogTrigger asChild>
                <Button size="sm" variant="outline" onClick={() => setSelectedEvidenceMilestone(row.original.id)}>
                  Submit Evidence
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>
                    Submit Milestone Evidence
                    {evidenceMilestoneQuery.data ? `: ${evidenceMilestoneQuery.data.name}` : ""}
                  </DialogTitle>
                </DialogHeader>
                {evidenceMilestoneQuery.data ? (
                  <p className="text-sm text-muted-foreground">
                    Status: {evidenceMilestoneQuery.data.status} | Retention: {formatMoney(evidenceMilestoneQuery.data.retentionAmount ?? 0, "TZS")}
                  </p>
                ) : null}
                <form
                  className="space-y-3"
                  onSubmit={evidenceForm.handleSubmit((values) => {
                    if (!selectedEvidenceMilestone) {
                      return;
                    }
                    submitEvidence.mutate({ milestoneId: selectedEvidenceMilestone, payload: values });
                  })}
                >
                  <Input placeholder="Evidence URI or reference" {...evidenceForm.register("evidenceUri")} />
                  <Input placeholder="Notes" {...evidenceForm.register("notes")} />
                  <Button type="submit" disabled={submitEvidence.isPending}>
                    {submitEvidence.isPending ? "Submitting..." : "Submit"}
                  </Button>
                </form>
              </DialogContent>
            </Dialog>
          </RoleGate>
          <RoleGate roles={["OWNER", "ADMIN"]}>
            <Button size="sm" onClick={() => approveSingle.mutate(row.original.id)} disabled={approveSingle.isPending}>Approve</Button>
          </RoleGate>
          <RoleGate roles={["OWNER", "ADMIN"]}>
            <Button size="sm" variant="outline" onClick={() => inspection.mutate(row.original.id)} disabled={inspection.isPending}>Inspect</Button>
          </RoleGate>
          <RoleGate roles={["OWNER", "ADMIN"]}>
            <Button size="sm" variant="ghost" onClick={() => retention.mutate(row.original.id)} disabled={retention.isPending}>Release Retention</Button>
          </RoleGate>
          <RoleGate roles={["OWNER", "ADMIN"]}>
            <Dialog>
              <DialogTrigger asChild>
                <Button size="sm" variant="outline" onClick={() => setSelectedSplitMilestone(row.original.id)}>Split Payout</Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>
                    Approve Multi-party Splits
                    {splitMilestoneQuery.data ? `: ${splitMilestoneQuery.data.name}` : ""}
                  </DialogTitle>
                </DialogHeader>
                {splitMilestoneQuery.data ? (
                  <p className="text-sm text-muted-foreground">
                    Gross: {formatMoney(splitMilestoneQuery.data.amount, "TZS")} | Status: {splitMilestoneQuery.data.status}
                  </p>
                ) : null}
                <form
                  className="space-y-3"
                  onSubmit={splitForm.handleSubmit((values) => selectedSplitMilestone && approveMulti.mutate({ milestoneId: selectedSplitMilestone, values }))}
                >
                  <Input placeholder="Supplier User ID" {...splitForm.register("supplierUserId")} />
                  <Input type="number" step="0.01" placeholder="Supplier Amount" {...splitForm.register("supplierAmount", { valueAsNumber: true })} />
                  <Input placeholder="Inspector User ID" {...splitForm.register("inspectorUserId")} />
                  <Input type="number" step="0.01" placeholder="Inspector Amount" {...splitForm.register("inspectorAmount", { valueAsNumber: true })} />
                  <Button type="submit" disabled={approveMulti.isPending}>{approveMulti.isPending ? "Approving..." : "Approve Multi"}</Button>
                </form>
              </DialogContent>
            </Dialog>
          </RoleGate>
        </div>
      )
    }
  ], [approveMulti, approveSingle, evidenceForm, inspection, retention, selectedEvidenceMilestone, selectedSplitMilestone, splitForm, submitEvidence]);

  if (projectQuery.error) {
    return <p className="text-sm text-red-700">{errorDetail(projectQuery.error, "Project detail endpoint unavailable in backend.")}</p>;
  }

  const milestoneItems = milestonesQuery.data?.content ?? [];
  const adaptedMilestones = adaptMilestones(milestoneItems);
  const canActivate = projectQuery.data?.status === "DRAFT";

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>{projectQuery.data?.title ?? "Project"}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          {projectQuery.isLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-4 w-32" />
              <Skeleton className="h-4 w-64" />
              <Skeleton className="h-4 w-64" />
            </div>
          ) : (
            <>
              <p>Status: {projectQuery.data?.status ?? "\u2014"}</p>
              <p>Owner: {formatUserLabel("Owner", projectQuery.data?.ownerUserId)}</p>
              <p>Contractor: {formatUserLabel("Contractor", projectQuery.data?.contractorUserId)}</p>
              <p>Inspector: {formatUserLabel("Inspector", projectQuery.data?.inspectorUserId)}</p>
            </>
          )}
          <RoleGate roles={["OWNER"]}>
            <div className="flex flex-wrap gap-2 pt-1">
              <Dialog
                open={showAssign}
                onOpenChange={(open) => {
                  setShowAssign(open);
                  if (open) {
                    assignmentForm.reset({
                      contractorUserId: projectQuery.data?.contractorUserId ?? "",
                      inspectorUserId: projectQuery.data?.inspectorUserId ?? ""
                    });
                  }
                }}
              >
                <DialogTrigger asChild>
                  <Button variant="outline" size="sm">Assign Participants</Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader><DialogTitle>Assign Contractor and Inspector</DialogTitle></DialogHeader>
                  <form
                    className="space-y-3"
                    onSubmit={assignmentForm.handleSubmit((values) => assignParticipantsMutation.mutate(values))}
                  >
                    <p className="text-sm text-muted-foreground">
                      Select an active contractor and inspector for this project assignment.
                    </p>
                    <div className="space-y-1">
                      <label className="text-sm font-medium" htmlFor="contractorUserId">Contractor</label>
                      <select
                        id="contractorUserId"
                        className="flex h-10 w-full rounded-md border border-input bg-card px-3 text-sm"
                        {...assignmentForm.register("contractorUserId")}
                        defaultValue={assignmentForm.getValues("contractorUserId")}
                      >
                        <option value="">Select contractor</option>
                        {(contractorsQuery.data ?? []).map((user) => (
                          <option key={user.id} value={user.id}>
                            {directoryLabel(user)}
                          </option>
                        ))}
                      </select>
                      {assignmentForm.formState.errors.contractorUserId ? (
                        <p className="text-xs text-red-700">{assignmentForm.formState.errors.contractorUserId.message}</p>
                      ) : (
                        <p className="text-xs text-muted-foreground">Only active contractor accounts are listed.</p>
                      )}
                    </div>
                    <div className="space-y-1">
                      <label className="text-sm font-medium" htmlFor="inspectorUserId">Inspector</label>
                      <select
                        id="inspectorUserId"
                        className="flex h-10 w-full rounded-md border border-input bg-card px-3 text-sm"
                        {...assignmentForm.register("inspectorUserId")}
                        defaultValue={assignmentForm.getValues("inspectorUserId")}
                      >
                        <option value="">Select inspector</option>
                        {(inspectorsQuery.data ?? []).map((user) => (
                          <option key={user.id} value={user.id}>
                            {directoryLabel(user)}
                          </option>
                        ))}
                      </select>
                      {assignmentForm.formState.errors.inspectorUserId ? (
                        <p className="text-xs text-red-700">{assignmentForm.formState.errors.inspectorUserId.message}</p>
                      ) : (
                        <p className="text-xs text-muted-foreground">Only active inspector accounts are listed.</p>
                      )}
                    </div>
                    {contractorsQuery.error || inspectorsQuery.error ? (
                      <p className="text-xs text-red-700">Assignable user directory unavailable.</p>
                    ) : null}
                    <div className="rounded-md border bg-muted/30 p-3 text-xs">
                      <p className="font-medium">Current project assignments</p>
                      <p className="mt-1">Owner: <span className="font-mono">{formatUserLabel("Owner", projectQuery.data?.ownerUserId)}</span></p>
                      <p>Contractor: <span className="font-mono">{formatUserLabel("Contractor", projectQuery.data?.contractorUserId)}</span></p>
                      <p>Inspector: <span className="font-mono">{formatUserLabel("Inspector", projectQuery.data?.inspectorUserId)}</span></p>
                    </div>
                    <Button type="submit" disabled={assignParticipantsMutation.isPending}>
                      {assignParticipantsMutation.isPending ? "Saving..." : "Save Assignment"}
                    </Button>
                  </form>
                </DialogContent>
              </Dialog>

              {canActivate ? (
                <Dialog open={showActivateConfirm} onOpenChange={setShowActivateConfirm}>
                  <DialogTrigger asChild>
                    <Button size="sm">Activate Project</Button>
                  </DialogTrigger>
                  <DialogContent>
                    <DialogHeader><DialogTitle>Activate Project</DialogTitle></DialogHeader>
                    <p className="text-sm text-muted-foreground">
                      This will activate the project and enable downstream milestone workflows.
                    </p>
                    <Button onClick={() => activateMutation.mutate()} disabled={activateMutation.isPending}>
                      {activateMutation.isPending ? "Activating..." : "Confirm Activate"}
                    </Button>
                  </DialogContent>
                </Dialog>
              ) : null}

              <Dialog open={showCreateMilestone} onOpenChange={setShowCreateMilestone}>
                <DialogTrigger asChild>
                  <Button variant="outline" size="sm">Create Milestone</Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader><DialogTitle>Create Milestone</DialogTitle></DialogHeader>
                  <form className="space-y-3" onSubmit={milestoneCreateForm.handleSubmit((values) => createMilestoneMutation.mutate(values))}>
                    <Input type="number" placeholder="Sequence number" {...milestoneCreateForm.register("sequenceNo", { valueAsNumber: true })} />
                    <Input placeholder="Milestone name" {...milestoneCreateForm.register("name")} />
                    <Input placeholder="Description (optional)" {...milestoneCreateForm.register("description")} />
                    <Input type="number" step="0.01" placeholder="Gross amount" {...milestoneCreateForm.register("amount", { valueAsNumber: true })} />
                    <Input type="number" step="0.01" placeholder="Retention amount (optional)" {...milestoneCreateForm.register("retentionAmount", { valueAsNumber: true })} />
                    <Input type="date" placeholder="Due date (optional)" {...milestoneCreateForm.register("dueDate")} />
                    <Button type="submit" disabled={createMilestoneMutation.isPending}>
                      {createMilestoneMutation.isPending ? "Creating..." : "Create Milestone"}
                    </Button>
                  </form>
                </DialogContent>
              </Dialog>
            </div>
          </RoleGate>
        </CardContent>
      </Card>

      {milestonesQuery.error ? (
        <p className="text-sm text-red-700">{errorDetail(milestonesQuery.error, "Milestones endpoint unavailable.")}</p>
      ) : milestonesQuery.isLoading ? (
        <MilestoneTimelineSkeleton />
      ) : (
        <MilestoneTimeline projectId={params.id} milestones={adaptedMilestones} />
      )}

      {milestonesQuery.error ? null : milestonesQuery.isLoading ? (
        <Card>
          <CardHeader><CardTitle>Milestone Actions</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            <TableSkeleton />
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          <DataTable columns={columns} data={milestoneItems} emptyLabel="No milestones" />
          <TablePagination
            page={milestonesPage}
            size={milestonePageSize}
            totalElements={milestonesQuery.data?.totalElements ?? 0}
            totalPages={milestonesQuery.data?.totalPages ?? 0}
            onPrevious={() => setMilestonesPage((value) => Math.max(0, value - 1))}
            onNext={() => setMilestonesPage((value) => value + 1)}
          />
        </div>
      )}
    </div>
  );
}
