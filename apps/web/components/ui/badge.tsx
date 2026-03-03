import { ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

type BadgeProps = {
  className?: string;
  children: ReactNode;
  variant?: "default" | "outline";
};

export function Badge({ className, children, variant = "default" }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex rounded-full px-2.5 py-1 text-xs font-medium",
        variant === "outline" ? "border border-border bg-card text-foreground" : "bg-muted text-foreground",
        className
      )}
    >
      {children}
    </span>
  );
}
