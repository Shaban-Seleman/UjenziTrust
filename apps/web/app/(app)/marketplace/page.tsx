import { ModulePlaceholder } from "@/components/layout/ModulePlaceholder";
import { sectionRoles } from "@/lib/auth/roles";

export default function MarketplacePage() {
  return (
    <ModulePlaceholder
      title="Marketplace"
      description="Placeholder page for listings, offers, and reservations."
      allow={[...sectionRoles.marketplace]}
    />
  );
}
