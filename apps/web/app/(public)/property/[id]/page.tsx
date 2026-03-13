import { redirect } from "next/navigation";
import { buildLoginHref } from "@/lib/auth/session";

export default function PublicPropertyDetailPage({ params }: { params: { id: string } }) {
  redirect(buildLoginHref(`/app/marketplace/properties/${params.id}`));
}
