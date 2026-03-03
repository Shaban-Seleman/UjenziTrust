"use client";

import { useQuery } from "@tanstack/react-query";
import { authMe } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/errors";

export function useActor() {
  const query = useQuery({
    queryKey: ["actor"],
    queryFn: authMe,
    retry: false,
    staleTime: 15_000
  });

  const actor = query.data;
  const roles = actor?.roles ?? [];
  const error = query.error instanceof ApiError ? query.error : null;
  const isUnauthorized = error?.status === 401;

  return {
    actor,
    roles,
    isLoading: query.isLoading,
    isAuthenticated: query.isSuccess && Boolean(actor),
    isUnauthorized,
    error: query.error,
    refetch: query.refetch
  };
}
