"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useActor } from "@/components/auth/useActor";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { hasAnyRole } from "@/lib/auth/roles";
import { isDemoModeEnabled } from "@/lib/config/demo";

type DemoQuickStartCardProps = {
  forceVisible?: boolean;
};

export function DemoQuickStartCard({ forceVisible = false }: DemoQuickStartCardProps) {
  const { roles } = useActor();
  const [isSeeded, setIsSeeded] = useState(forceVisible);
  const [seededAt, setSeededAt] = useState<string | null>(null);
  const [scenario, setScenario] = useState<string | null>(null);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    const storedAt = window.sessionStorage.getItem("demo_seeded_at");
    const storedScenario = window.sessionStorage.getItem("demo_scenario");
    if (storedAt) {
      setIsSeeded(true);
      setSeededAt(storedAt);
    }
    if (storedScenario) {
      setScenario(storedScenario);
    }

    const onSeeded = (event: Event) => {
      const payload = (event as CustomEvent<{ seededAt?: string; scenario?: string }>).detail;
      setIsSeeded(true);
      if (payload?.seededAt) {
        setSeededAt(payload.seededAt);
      }
      if (payload?.scenario) {
        setScenario(payload.scenario);
      }
    };

    window.addEventListener("demo-seeded", onSeeded as EventListener);
    return () => {
      window.removeEventListener("demo-seeded", onSeeded as EventListener);
    };
  }, []);

  useEffect(() => {
    if (forceVisible) {
      setIsSeeded(true);
    }
  }, [forceVisible]);

  if (!isDemoModeEnabled() || !hasAnyRole(roles, ["ADMIN"]) || !isSeeded) {
    return null;
  }

  return (
    <Card>
      <CardHeader className="space-y-2">
        <div className="flex flex-wrap items-center gap-2">
          <CardTitle className="text-base">Investor Demo Ready</CardTitle>
          {scenario ? <Badge variant="outline">{scenario}</Badge> : null}
        </div>
        {seededAt ? <p className="text-xs text-muted-foreground">Seeded at {new Date(seededAt).toLocaleString()}</p> : null}
      </CardHeader>
      <CardContent className="space-y-4">
        <ul className="list-disc space-y-1 pl-5 text-sm text-muted-foreground">
          <li>Marketplace has accepted offers</li>
          <li>Escrows span all lifecycle states</li>
          <li>Construction includes multi-party milestone payouts</li>
          <li>Retention includes ready and future release dates</li>
        </ul>
        <Separator />
        <div className="flex flex-wrap gap-2">
          <Link href="/app/marketplace/properties"><Button size="sm">Open Marketplace</Button></Link>
          <Link href="/app/escrows"><Button size="sm" variant="outline">Open Escrows</Button></Link>
          <Link href="/app/construction/projects"><Button size="sm" variant="outline">Open Construction</Button></Link>
        </div>
      </CardContent>
    </Card>
  );
}
