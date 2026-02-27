import { authMe } from "@/lib/api/endpoints";

export async function getActorSession() {
  return authMe();
}
