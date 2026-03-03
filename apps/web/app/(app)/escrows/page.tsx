import { ModulePlaceholder } from "@/components/layout/ModulePlaceholder";
import { sectionRoles } from "@/lib/auth/roles";

export default function EscrowsPage() {
  return (
    <ModulePlaceholder
      title="Escrows"
      description="Placeholder page for escrow balances, disbursements, and audit views."
      allow={[...sectionRoles.escrows]}
    />
  );
}
