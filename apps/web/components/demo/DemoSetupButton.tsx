"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { DatabaseZap, Loader2, PlayCircle } from "lucide-react";
import { toast } from "sonner";
import { useActor } from "@/components/auth/useActor";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Separator } from "@/components/ui/separator";
import { resetAndSeedDemoScenario } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/errors";
import type { DemoSeedSummary } from "@/lib/api/schemas";
import { hasAnyRole } from "@/lib/auth/roles";
import { isDemoModeEnabled } from "@/lib/config/demo";

const SCENARIO = "investor_v1";

function toErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 403 || error.status === 404) {
      return "Demo setup is not available in this environment.";
    }
    return error.detail || "Demo setup failed.";
  }
  if (error instanceof TypeError) {
    return "Unable to contact demo setup service.";
  }
  return "Demo setup failed. Please retry.";
}

export function DemoSetupButton() {
  const router = useRouter();
  const { roles } = useActor();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [resultOpen, setResultOpen] = useState(false);
  const [summary, setSummary] = useState<DemoSeedSummary | null>(null);

  const mutation = useMutation({
    mutationFn: () => resetAndSeedDemoScenario(SCENARIO),
    onSuccess: (data) => {
      setSummary(data);
      setConfirmOpen(false);
      setResultOpen(true);
      if (typeof window !== "undefined") {
        const seededAt = new Date().toISOString();
        window.sessionStorage.setItem("demo_seeded_at", seededAt);
        window.sessionStorage.setItem("demo_scenario", SCENARIO);
        window.dispatchEvent(new CustomEvent("demo-seeded", { detail: { seededAt, scenario: SCENARIO } }));
      }
      toast.success(
        `Demo data ready: ${data.propertiesCreated} properties, ${data.offersCreated} offers, ${data.escrowsCreated} escrows, ${data.projectsCreated} projects`
      );
    },
    onError: (error) => {
      toast.error(toErrorMessage(error));
    }
  });

  const isAdmin = hasAnyRole(roles, ["ADMIN"]);
  const canRender = isDemoModeEnabled() && isAdmin;
  const summaryRows = useMemo(
    () =>
      summary
        ? [
            ["Users", summary.usersCreated],
            ["Properties", summary.propertiesCreated],
            ["Offers", summary.offersCreated],
            ["Escrows", summary.escrowsCreated],
            ["Projects", summary.projectsCreated],
            ["Milestones", summary.milestonesCreated],
            ["Disbursements", summary.disbursementsCreated],
            ["Settled", summary.settledDisbursements],
            ["Retention Ready", summary.retentionReadyCount]
          ]
        : [],
    [summary]
  );

  if (!canRender) {
    return null;
  }

  return (
    <>
      <Card>
        <CardHeader className="space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <CardTitle className="text-base">Local Demo Setup</CardTitle>
            <Badge variant="outline">ADMIN</Badge>
            <Badge variant="outline">LOCAL ONLY</Badge>
          </div>
          <p className="text-sm text-muted-foreground">
            Reset local data and seed the investor demo scenario.
          </p>
        </CardHeader>
        <CardContent>
          <Button onClick={() => setConfirmOpen(true)} disabled={mutation.isPending} className="gap-2">
            {mutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <DatabaseZap className="h-4 w-4" />}
            Reset & Seed Demo Data
          </Button>
        </CardContent>
      </Card>

      <Dialog open={confirmOpen} onOpenChange={(open) => !mutation.isPending && setConfirmOpen(open)}>
        <DialogContent
          onEscapeKeyDown={mutation.isPending ? (event) => event.preventDefault() : undefined}
          onInteractOutside={mutation.isPending ? (event) => event.preventDefault() : undefined}
        >
          <DialogHeader>
            <DialogTitle>Reset local demo data?</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            This will clear current local data and generate a fresh investor demo scenario.
          </p>
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={() => setConfirmOpen(false)} disabled={mutation.isPending}>
              Cancel
            </Button>
            <Button onClick={() => mutation.mutate()} disabled={mutation.isPending} className="gap-2">
              {mutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              Continue
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog open={resultOpen} onOpenChange={setResultOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Demo data seeded</DialogTitle>
          </DialogHeader>
          {summary ? (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-2">
                {summaryRows.map(([label, value]) => (
                  <div key={label} className="rounded-md border bg-muted/30 px-3 py-2">
                    <p className="text-xs text-muted-foreground">{label}</p>
                    <p className="text-sm font-semibold">{value}</p>
                  </div>
                ))}
              </div>
              <Separator />
              <div className="flex flex-wrap gap-2">
                <Button
                  className="gap-2"
                  onClick={() => {
                    setResultOpen(false);
                    router.push("/dashboard");
                  }}
                >
                  <PlayCircle className="h-4 w-4" />
                  Start Demo
                </Button>
                <Button variant="outline" onClick={() => setResultOpen(false)}>
                  Stay Here
                </Button>
              </div>
              <div className="flex flex-wrap gap-2">
                <Link href="/app/marketplace/properties"><Button size="sm" variant="outline">Open Marketplace</Button></Link>
                <Link href="/app/escrows"><Button size="sm" variant="outline">Open Escrows</Button></Link>
                <Link href="/app/construction/projects"><Button size="sm" variant="outline">Open Construction</Button></Link>
              </div>
            </div>
          ) : null}
        </DialogContent>
      </Dialog>
    </>
  );
}
