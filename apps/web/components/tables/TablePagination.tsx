"use client";

import { Button } from "@/components/ui/button";

type TablePaginationProps = {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  onPrevious: () => void;
  onNext: () => void;
};

export function TablePagination({
  page,
  size,
  totalElements,
  totalPages,
  onPrevious,
  onNext
}: TablePaginationProps) {
  const startItem = totalElements === 0 ? 0 : page * size + 1;
  const endItem = totalElements === 0 ? 0 : Math.min((page + 1) * size, totalElements);

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-border bg-card p-4 md:flex-row md:items-center md:justify-between">
      <p className="text-sm text-muted-foreground">
        Showing {startItem}-{endItem} of {totalElements} results
      </p>
      <div className="flex items-center gap-2">
        <span className="text-sm text-muted-foreground">
          Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
        </span>
        <Button variant="outline" size="sm" onClick={onPrevious} disabled={page === 0}>
          Previous
        </Button>
        <Button variant="outline" size="sm" onClick={onNext} disabled={totalPages === 0 || page >= totalPages - 1}>
          Next
        </Button>
      </div>
    </div>
  );
}
