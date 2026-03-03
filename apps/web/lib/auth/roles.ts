export const appRoles = ["OWNER", "SELLER", "BUYER", "CONTRACTOR", "INSPECTOR", "ADMIN"] as const;
export type AppRole = (typeof appRoles)[number];

export const sectionRoles = {
  dashboard: ["OWNER", "SELLER", "BUYER", "CONTRACTOR", "INSPECTOR", "ADMIN"],
  marketplace: ["OWNER", "SELLER", "BUYER", "ADMIN"],
  escrows: ["OWNER", "SELLER", "BUYER", "ADMIN"],
  construction: ["OWNER", "CONTRACTOR", "INSPECTOR", "ADMIN"],
  admin: ["ADMIN"]
} as const satisfies Record<string, AppRole[]>;

export const protectedRouteRoles = [
  { prefix: "/admin", roles: sectionRoles.admin },
  { prefix: "/marketplace", roles: sectionRoles.marketplace },
  { prefix: "/escrows", roles: sectionRoles.escrows },
  { prefix: "/construction", roles: sectionRoles.construction },
  { prefix: "/dashboard", roles: sectionRoles.dashboard },
  { prefix: "/app/admin", roles: sectionRoles.admin },
  { prefix: "/app/marketplace", roles: sectionRoles.marketplace },
  { prefix: "/app/escrows", roles: sectionRoles.escrows },
  { prefix: "/app/construction", roles: sectionRoles.construction },
  { prefix: "/app/dashboard", roles: sectionRoles.dashboard }
] as const;

export function hasAnyRole(actorRoles: string[] = [], required: AppRole[]) {
  return required.some((role) => actorRoles.includes(role));
}
