"use client";

import { useQuery } from "@tanstack/react-query";
import { getProperty } from "@/lib/api/endpoints";

export default function PublicPropertyDetailPage({ params }: { params: { id: string } }) {
  const query = useQuery({ queryKey: ["property", params.id, "public"], queryFn: () => getProperty(params.id) });

  if (query.isLoading) {
    return <div className="p-6 text-sm text-muted-foreground">Loading property...</div>;
  }

  if (query.error || !query.data) {
    return <div className="p-6 text-sm text-red-700">Property not found.</div>;
  }

  const property = query.data;

  return (
    <div className="mx-auto max-w-4xl space-y-3 p-6">
      <h1 className="text-2xl font-semibold">{property.title}</h1>
      <p className="text-muted-foreground">{property.location}</p>
      <p className="text-sm">Status: {property.status}</p>
      <p className="text-sm">Currency: {property.currency}</p>
      <p className="text-sm">Price: {property.askingPrice ?? "N/A"}</p>
      <p>{property.description}</p>
    </div>
  );
}
