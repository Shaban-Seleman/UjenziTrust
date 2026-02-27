import { AppRole, hasAnyRole } from "@/lib/auth/roles";

export function ensureRole(roles: string[] | undefined, required: AppRole[]) {
  return hasAnyRole(roles ?? [], required);
}

export function canAccessPath(pathname: string, roles: string[] | undefined) {
  if (pathname.startsWith("/app/admin")) {
    return ensureRole(roles, ["ADMIN"]);
  }
  if (pathname.startsWith("/app/marketplace")) {
    return ensureRole(roles, ["OWNER", "SELLER", "BUYER", "ADMIN"]);
  }
  if (pathname.startsWith("/app/escrows")) {
    return ensureRole(roles, ["OWNER", "SELLER", "BUYER", "ADMIN"]);
  }
  if (pathname.startsWith("/app/construction")) {
    return ensureRole(roles, ["OWNER", "CONTRACTOR", "INSPECTOR", "ADMIN"]);
  }
  if (pathname.startsWith("/app/dashboard")) {
    return Boolean(roles?.length);
  }
  return true;
}
