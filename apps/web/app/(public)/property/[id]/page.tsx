import { redirect } from "next/navigation";

export default function PublicPropertyDetailPage({ params }: { params: { id: string } }) {
  redirect(`/app/marketplace/properties/${params.id}`);
}
