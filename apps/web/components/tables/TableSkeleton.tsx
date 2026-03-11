"use client";

import { Skeleton } from "@/components/ui/skeleton";

type TableSkeletonProps = {
  rows?: number;
};

export function TableSkeleton({ rows = 3 }: TableSkeletonProps) {
  return (
    <div className="space-y-2">
      {Array.from({ length: rows }).map((_, index) => (
        <Skeleton key={index} className="h-10 w-full" />
      ))}
    </div>
  );
}
