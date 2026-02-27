export function formatMoney(amount: number | string, currency = "TZS"): string {
  const value = typeof amount === "string" ? Number(amount) : amount;
  return new Intl.NumberFormat("en-TZ", {
    style: "currency",
    currency,
    maximumFractionDigits: 2
  }).format(value);
}
