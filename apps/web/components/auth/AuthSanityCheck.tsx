"use client";

import { usePathname } from "next/navigation";
import { useActor } from "@/components/auth/useActor";

export function AuthSanityCheck() {
  const pathname = usePathname();
  const { actor, isLoading, isAuthenticated, isUnauthorized } = useActor();

  if (process.env.NODE_ENV === "production") {
    return null;
  }

  return (
    <div className="rounded-lg border border-dashed border-border bg-card p-4 text-xs text-muted-foreground">
      <p className="font-medium text-foreground">Auth sanity check</p>
      <p>Path: {pathname}</p>
      <p>Loading: {String(isLoading)}</p>
      <p>Authenticated: {String(isAuthenticated)}</p>
      <p>Unauthorized: {String(isUnauthorized)}</p>
      <p>User: {actor?.userId ?? "none"}</p>
    </div>
  );
}
