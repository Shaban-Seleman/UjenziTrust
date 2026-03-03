"use client";

import { useQueries } from "@tanstack/react-query";
import { AuthSanityCheck } from "@/components/auth/AuthSanityCheck";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { listEscrows, listOffers, listProjects, listProperties } from "@/lib/api/endpoints";

function LoadingCards() {
  return (
    <div className="grid gap-4 md:grid-cols-4">
      {Array.from({ length: 4 }).map((_, index) => (
        <Card key={index}>
          <CardHeader>
            <div className="h-4 w-24 animate-pulse rounded bg-muted" />
          </CardHeader>
          <CardContent>
            <div className="h-8 w-12 animate-pulse rounded bg-muted" />
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

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
  const isLoading = results.some((result) => result.isLoading);
  const hasError = results.some((result) => result.isError);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Dashboard</h1>
      {isLoading ? (
        <LoadingCards />
      ) : hasError ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          Unable to load dashboard metrics right now. Retry once the backend is available.
        </div>
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-4">
            <Card><CardHeader><CardTitle className="text-sm">Properties</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{properties.data?.length ?? 0}</CardContent></Card>
            <Card><CardHeader><CardTitle className="text-sm">Offers</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{offers.data?.length ?? 0}</CardContent></Card>
            <Card><CardHeader><CardTitle className="text-sm">Escrows</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{escrows.data?.length ?? 0}</CardContent></Card>
            <Card><CardHeader><CardTitle className="text-sm">Projects</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{projects.data?.length ?? 0}</CardContent></Card>
          </div>
          <p className="text-sm text-muted-foreground">Counts reflect successful API responses only.</p>
        </>
      )}
      <AuthSanityCheck />
    </div>
  );
}
