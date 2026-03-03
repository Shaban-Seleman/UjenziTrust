"use client";

import { useEffect, useRef } from "react";
import type { Route } from "next";
import { usePathname, useRouter } from "next/navigation";
import { AUTH_REQUIRED_EVENT, buildLoginHref } from "@/lib/auth/session";

export function AuthRedirectHandler() {
  const router = useRouter();
  const pathname = usePathname();
  const lastRedirectRef = useRef<string | null>(null);

  useEffect(() => {
    const handleAuthRequired = () => {
      if (pathname === "/login") {
        return;
      }

      const query = typeof window === "undefined" ? "" : window.location.search;
      const nextPath = `${pathname}${query}`;
      const loginHref = buildLoginHref(nextPath);
      if (lastRedirectRef.current === loginHref) {
        return;
      }

      lastRedirectRef.current = loginHref;
      router.replace(loginHref as Route);
    };

    window.addEventListener(AUTH_REQUIRED_EVENT, handleAuthRequired);
    return () => window.removeEventListener(AUTH_REQUIRED_EVENT, handleAuthRequired);
  }, [pathname, router]);

  return null;
}
