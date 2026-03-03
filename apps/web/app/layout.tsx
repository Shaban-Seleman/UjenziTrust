import type { Metadata } from "next";
import { ReactNode } from "react";
import "@/app/globals.css";
import { QueryProvider } from "@/lib/query/providers";

export const metadata: Metadata = {
  title: process.env.NEXT_PUBLIC_APP_NAME ?? "NyumbaTrust",
  description: "Tanzania real estate + escrow + construction management"
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  );
}
