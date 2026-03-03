import { AppRole, hasAnyRole, protectedRouteRoles } from "@/lib/auth/roles";

export function ensureRole(roles: string[] | undefined, required: AppRole[]) {
  return hasAnyRole(roles ?? [], required);
}

export function canAccessPath(pathname: string, roles: string[] | undefined) {
  for (const rule of protectedRouteRoles) {
    if (pathname.startsWith(rule.prefix)) {
      return ensureRole(roles, [...rule.roles]);
    }
  }

  return true;
}
