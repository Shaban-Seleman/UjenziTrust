import Link from "next/link";
import type { Route } from "next";
import { Button } from "@/components/ui/button";
import { buildLoginHref } from "@/lib/auth/session";

export default function PublicHomePage() {
  return (
    <div className="mx-auto max-w-5xl px-6 py-20">
      <h1 className="text-4xl font-semibold tracking-tight">NyumbaTrust</h1>
      <p className="mt-4 max-w-2xl text-muted-foreground">
        Real estate marketplace, escrow lifecycle, and construction milestone payouts for Tanzania.
      </p>
      <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
        Marketplace browsing requires sign-in. Continue to dashboard to access properties, offers, reservations, and escrow workflows.
      </p>
      <div className="mt-6 flex gap-3">
        <Link href="/login"><Button>Open Dashboard</Button></Link>
        <Link href={buildLoginHref("/app/marketplace/properties") as Route}><Button variant="outline">Go to Marketplace</Button></Link>
      </div>
    </div>
  );
}
