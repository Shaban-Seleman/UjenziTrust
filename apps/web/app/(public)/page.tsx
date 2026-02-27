import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function PublicHomePage() {
  return (
    <div className="mx-auto max-w-5xl px-6 py-20">
      <h1 className="text-4xl font-semibold tracking-tight">NyumbaTrust</h1>
      <p className="mt-4 max-w-2xl text-muted-foreground">
        Real estate marketplace, escrow lifecycle, and construction milestone payouts for Tanzania.
      </p>
      <div className="mt-6 flex gap-3">
        <Link href="/login"><Button>Open Dashboard</Button></Link>
        <Link href="/properties"><Button variant="outline">Browse Listings</Button></Link>
      </div>
    </div>
  );
}
