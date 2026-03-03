"use client";

import { ReactNode } from "react";
import { PropsWithChildren } from "react";
import { useActor } from "@/components/auth/useActor";
import { AppRole, hasAnyRole } from "@/lib/auth/roles";

type RoleGateProps = PropsWithChildren<{
  allow?: AppRole[];
  roles?: AppRole[];
  fallback?: ReactNode;
}>;

export function RoleGate({ allow, roles, children, fallback = null }: RoleGateProps) {
  const { roles: actorRoles } = useActor();
  const requiredRoles = allow ?? roles ?? [];

  if (!hasAnyRole(actorRoles, requiredRoles)) {
    return <>{fallback}</>;
  }
  return <>{children}</>;
}
