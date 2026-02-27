"use client";

import { useQueries } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { listEscrows, listOffers, listProjects, listProperties } from "@/lib/api/endpoints";

export default function DashboardPage() {
  const results = useQueries({
    queries: [
      { queryKey: ["properties"], queryFn: listProperties },
      { queryKey: ["offers"], queryFn: listOffers },
      { queryKey: ["escrows"], queryFn: listEscrows },
      { queryKey: ["projects"], queryFn: listProjects }
    ]
  });

  const [properties, offers, escrows, projects] = results;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Dashboard</h1>
      <div className="grid gap-4 md:grid-cols-4">
        <Card><CardHeader><CardTitle className="text-sm">Properties</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{properties.data?.length ?? 0}</CardContent></Card>
        <Card><CardHeader><CardTitle className="text-sm">Offers</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{offers.data?.length ?? 0}</CardContent></Card>
        <Card><CardHeader><CardTitle className="text-sm">Escrows</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{escrows.data?.length ?? 0}</CardContent></Card>
        <Card><CardHeader><CardTitle className="text-sm">Projects</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{projects.data?.length ?? 0}</CardContent></Card>
      </div>
      <p className="text-sm text-muted-foreground">Counts may show zero if corresponding list endpoints are not enabled in backend yet.</p>
    </div>
  );
}
