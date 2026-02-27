"use client";

import { PropsWithChildren, useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import { useActor } from "@/components/auth/useActor";
import { canAccessPath } from "@/lib/auth/guards";

export function RequireAuth({ children }: PropsWithChildren) {
  const { isLoading, isAuthenticated, roles } = useActor();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [isAuthenticated, isLoading, pathname, router]);

  if (isLoading) {
    return <div className="p-6 text-sm text-muted-foreground">Loading session...</div>;
  }

  if (!isAuthenticated) {
    return null;
  }

  if (!canAccessPath(pathname, roles)) {
    return <div className="p-6 text-sm text-red-700">Not authorized for this section.</div>;
  }

  return <>{children}</>;
}
