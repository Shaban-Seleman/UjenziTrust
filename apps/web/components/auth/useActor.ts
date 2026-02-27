"use client";

import { useQuery } from "@tanstack/react-query";
import { authMe } from "@/lib/api/endpoints";

export function useActor() {
  const query = useQuery({
    queryKey: ["actor"],
    queryFn: authMe,
    retry: false
  });

  const actor = query.data;
  const roles = actor?.roles ?? [];

  return {
    actor,
    roles,
    isLoading: query.isLoading,
    isAuthenticated: Boolean(actor),
    error: query.error,
    refetch: query.refetch
  };
}
