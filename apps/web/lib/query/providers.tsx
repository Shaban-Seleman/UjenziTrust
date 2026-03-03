"use client";

import { PropsWithChildren, useState } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "sonner";
import { AuthRedirectHandler } from "@/components/auth/AuthRedirectHandler";
import { createQueryClient } from "@/lib/query/queryClient";

export function QueryProvider({ children }: PropsWithChildren) {
  const [queryClient] = useState(createQueryClient);

  return (
    <QueryClientProvider client={queryClient}>
      <AuthRedirectHandler />
      {children}
      <Toaster richColors position="top-right" />
    </QueryClientProvider>
  );
}

export const Providers = QueryProvider;
