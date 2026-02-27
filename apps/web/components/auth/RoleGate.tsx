"use client";

import { ReactNode } from "react";
import { PropsWithChildren } from "react";
import { useActor } from "@/components/auth/useActor";
import { AppRole, hasAnyRole } from "@/lib/auth/roles";

export function RoleGate({ roles, children, fallback = null }: PropsWithChildren<{ roles: AppRole[]; fallback?: ReactNode }>) {
  const { roles: actorRoles } = useActor();

  if (!hasAnyRole(actorRoles, roles)) {
    return <>{fallback}</>;
  }
  return <>{children}</>;
}
