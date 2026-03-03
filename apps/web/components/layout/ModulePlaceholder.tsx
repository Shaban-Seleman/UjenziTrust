"use client";

import { RoleGate } from "@/components/auth/RoleGate";
import { useActor } from "@/components/auth/useActor";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { AppRole } from "@/lib/auth/roles";

type ModulePlaceholderProps = {
  title: string;
  description: string;
  allow: AppRole[];
};

export function ModulePlaceholder({ title, description, allow }: ModulePlaceholderProps) {
  const { actor, roles } = useActor();

  return (
    <RoleGate
      allow={allow}
      fallback={<div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">Not authorized.</div>}
    >
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          <p className="text-sm text-muted-foreground">{description}</p>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Authenticated Actor</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              <p><span className="font-medium">User ID:</span> {actor?.userId ?? "Unknown"}</p>
              <p><span className="font-medium">Roles:</span> {roles.join(", ") || "None"}</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Module Status</CardTitle>
            </CardHeader>
            <CardContent className="text-sm text-muted-foreground">
              Package 1 provides the secured shell, auth flow, and proxy foundation. Module-specific UI lands in later packages.
            </CardContent>
          </Card>
        </div>
      </div>
    </RoleGate>
  );
}
