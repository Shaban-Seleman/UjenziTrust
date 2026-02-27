"use client";

import { ComponentType } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, Building2, Wallet, Hammer, Shield } from "lucide-react";
import { useActor } from "@/components/auth/useActor";
import { AppRole, hasAnyRole } from "@/lib/auth/roles";
import { cn } from "@/lib/utils/cn";

type NavItem = {
  href: string;
  label: string;
  icon: ComponentType<{ className?: string }>;
  roles: AppRole[];
};

const navItems: NavItem[] = [
  { href: "/app/dashboard", label: "Dashboard", icon: LayoutDashboard, roles: ["OWNER", "SELLER", "BUYER", "CONTRACTOR", "INSPECTOR", "ADMIN"] },
  { href: "/app/marketplace/properties", label: "Marketplace", icon: Building2, roles: ["OWNER", "SELLER", "BUYER", "ADMIN"] },
  { href: "/app/escrows", label: "Escrows", icon: Wallet, roles: ["OWNER", "SELLER", "BUYER", "ADMIN"] },
  { href: "/app/construction/projects", label: "Construction", icon: Hammer, roles: ["OWNER", "CONTRACTOR", "INSPECTOR", "ADMIN"] },
  { href: "/app/admin", label: "Admin", icon: Shield, roles: ["ADMIN"] }
];

export function Sidebar() {
  const pathname = usePathname();
  const { roles } = useActor();

  return (
    <aside className="w-full border-r border-border bg-card md:w-72">
      <div className="border-b border-border p-5">
        <p className="font-semibold tracking-tight">NyumbaTrust Console</p>
        <p className="text-xs text-muted-foreground">Tanzania real estate and escrow</p>
      </div>
      <nav className="space-y-1 p-3">
        {navItems.filter((item) => hasAnyRole(roles, item.roles)).map((item) => {
          const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-2 rounded-md px-3 py-2 text-sm",
                active ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground"
              )}
            >
              <Icon className="h-4 w-4" />
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
