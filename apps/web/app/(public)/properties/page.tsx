import { redirect } from "next/navigation";
import { buildLoginHref } from "@/lib/auth/session";

export default function PublicPropertiesPage() {
  redirect(buildLoginHref("/app/marketplace/properties"));
}
