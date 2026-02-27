export const appRoles = ["OWNER", "SELLER", "BUYER", "CONTRACTOR", "INSPECTOR", "ADMIN"] as const;
export type AppRole = (typeof appRoles)[number];

export function hasAnyRole(actorRoles: string[] = [], required: AppRole[]) {
  return required.some((role) => actorRoles.includes(role));
}
