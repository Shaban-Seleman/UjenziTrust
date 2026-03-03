"use client";

import { PropsWithChildren, useEffect } from "react";
import type { Route } from "next";
import { useRouter, usePathname } from "next/navigation";
import { useActor } from "@/components/auth/useActor";
import { canAccessPath } from "@/lib/auth/guards";
import { buildLoginHref } from "@/lib/auth/session";

export function RequireAuth({ children }: PropsWithChildren) {
  const { isLoading, isAuthenticated, isUnauthorized, roles, error } = useActor();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && isUnauthorized) {
      const query = typeof window === "undefined" ? "" : window.location.search.replace(/^\?/, "");
      const nextPath = query ? `${pathname}?${query}` : pathname;
      router.replace(buildLoginHref(nextPath) as Route);
    }
  }, [isLoading, isUnauthorized, pathname, router]);

  if (isLoading) {
    return <div className="p-6 text-sm text-muted-foreground">Loading session...</div>;
  }

  if (error && !isUnauthorized) {
    return <div className="p-6 text-sm text-red-700">Unable to verify your session right now.</div>;
  }

  if (!isAuthenticated) {
    return <div className="p-6 text-sm text-muted-foreground">Redirecting to sign in...</div>;
  }

  if (!canAccessPath(pathname, roles)) {
    return <div className="p-6 text-sm text-red-700">Not authorized for this section.</div>;
  }

  return <>{children}</>;
}
