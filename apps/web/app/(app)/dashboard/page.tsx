"use client";

import { useActor } from "@/components/auth/useActor";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function DashboardPage() {
  const { actor, roles } = useActor();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
        <p className="text-sm text-muted-foreground">Protected foundation landing page for NyumbaTrust.</p>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Session</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            <p><span className="font-medium">User ID:</span> {actor?.userId ?? "Unknown"}</p>
            <p><span className="font-medium">Roles:</span> {roles.join(", ") || "None"}</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Auth Model</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            The browser talks only to Next.js routes. The token stays in an HttpOnly cookie and the proxy injects the Bearer header.
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Foundation Status</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            Package 1 is focused on auth, routing, shell layout, and module placeholders only.
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
