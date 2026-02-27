"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { listProperties } from "@/lib/api/endpoints";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function PublicPropertiesPage() {
  const query = useQuery({ queryKey: ["properties", "public"], queryFn: listProperties });

  if (query.isLoading) {
    return <div className="p-6 text-sm text-muted-foreground">Loading properties...</div>;
  }

  if (query.error) {
    return <div className="p-6 text-sm text-red-700">Unable to load listings.</div>;
  }

  return (
    <div className="mx-auto max-w-5xl space-y-4 p-6">
      <h1 className="text-2xl font-semibold">Public Listings</h1>
      {(query.data ?? []).map((property) => (
        <Card key={property.id}>
          <CardHeader>
            <CardTitle>{property.title}</CardTitle>
          </CardHeader>
          <CardContent className="flex items-center justify-between text-sm">
            <div>
              <p>{property.location ?? "Unknown location"}</p>
              <p className="text-muted-foreground">{property.status}</p>
            </div>
            <Link className="text-primary" href={`/property/${property.id}`}>View</Link>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
