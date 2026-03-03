import { ModulePlaceholder } from "@/components/layout/ModulePlaceholder";
import { sectionRoles } from "@/lib/auth/roles";

export default function AdminPage() {
  return (
    <ModulePlaceholder
      title="Admin"
      description="Placeholder page for operational oversight and administrative tooling."
      allow={[...sectionRoles.admin]}
    />
  );
}
