"use client";

import { Card, CardContent } from "@/components/ui/card";

export type TableSummaryItem = {
  label: string;
  value: number | string;
  hint: string;
};

type TableSummaryCardsProps = {
  items: TableSummaryItem[];
};

export function TableSummaryCards({ items }: TableSummaryCardsProps) {
  const gridClass =
    items.length >= 4 ? "md:grid-cols-4" : items.length === 2 ? "md:grid-cols-2" : "md:grid-cols-3";

  return (
    <div className={`grid gap-3 ${gridClass}`}>
      {items.map((item) => (
        <Card key={item.label}>
          <CardContent className="space-y-1 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-muted-foreground">{item.label}</p>
            <p className="text-2xl font-semibold">{item.value}</p>
            <p className="text-sm text-muted-foreground">{item.hint}</p>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
