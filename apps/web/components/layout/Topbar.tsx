"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { useActor } from "@/components/auth/useActor";
import { logout as logoutRequest } from "@/lib/api/endpoints";

export function Topbar() {
  const { actor } = useActor();
  const queryClient = useQueryClient();
  const router = useRouter();

  const logout = useMutation({
    mutationFn: () => logoutRequest(),
    onSuccess: () => {
      queryClient.clear();
      router.replace("/login");
    },
    onError: (error: unknown) => {
      toast.error(error instanceof Error ? error.message : "Logout failed");
    }
  });

  return (
    <header className="flex h-14 items-center justify-between border-b border-border bg-card px-4">
      <p className="text-sm text-muted-foreground">{actor?.userId ?? ""}</p>
      <div className="flex items-center gap-2">
        {(actor?.roles ?? []).map((role) => (
          <span key={role} className="rounded bg-muted px-2 py-1 text-xs">{role}</span>
        ))}
        <Button variant="outline" size="sm" onClick={() => logout.mutate()} disabled={logout.isPending}>
          Logout
        </Button>
      </div>
    </header>
  );
}
