import { ModulePlaceholder } from "@/components/layout/ModulePlaceholder";
import { sectionRoles } from "@/lib/auth/roles";

export default function ConstructionPage() {
  return (
    <ModulePlaceholder
      title="Construction"
      description="Placeholder page for projects, milestones, inspections, and retention."
      allow={[...sectionRoles.construction]}
    />
  );
}
