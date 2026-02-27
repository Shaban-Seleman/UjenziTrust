"use client";

import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { RoleGate } from "@/components/auth/RoleGate";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { getProperty, publishProperty, submitOffer } from "@/lib/api/endpoints";
import { formatMoney } from "@/lib/utils/money";

const offerSchema = z.object({ amount: z.coerce.number().positive() });
type OfferForm = z.infer<typeof offerSchema>;

export default function PropertyDetailPage({ params }: { params: { id: string } }) {
  const queryClient = useQueryClient();
  const propertyQuery = useQuery({ queryKey: ["property", params.id], queryFn: () => getProperty(params.id) });

  const form = useForm<OfferForm>({ resolver: zodResolver(offerSchema), defaultValues: { amount: 0 } });

  const publish = useMutation({
    mutationFn: () => publishProperty(params.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["property", params.id] });
      toast.success("Property published");
    },
    onError: () => toast.error("Publish failed")
  });

  const makeOffer = useMutation({
    mutationFn: (data: OfferForm) => submitOffer(params.id, { amount: data.amount, currency: "TZS" }),
    onSuccess: () => {
      form.reset({ amount: 0 });
      toast.success("Offer submitted");
    },
    onError: () => toast.error("Offer submission failed")
  });

  if (propertyQuery.isLoading) {
    return <p className="text-sm text-muted-foreground">Loading property...</p>;
  }

  if (propertyQuery.error || !propertyQuery.data) {
    return <p className="text-sm text-red-700">Property unavailable.</p>;
  }

  const property = propertyQuery.data;

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader><CardTitle>{property.title}</CardTitle></CardHeader>
        <CardContent className="space-y-2 text-sm">
          <p>{property.description}</p>
          <p>Location: {property.location ?? "-"}</p>
          <p>Price: {formatMoney(property.askingPrice ?? 0, property.currency ?? "TZS")}</p>
          <p>Status: {property.status}</p>
          <RoleGate roles={["SELLER", "OWNER", "ADMIN"]}>
            <Button onClick={() => publish.mutate()} disabled={publish.isPending}>Publish</Button>
          </RoleGate>
        </CardContent>
      </Card>
      <RoleGate roles={["BUYER"]}>
        <Card className="max-w-md">
          <CardHeader><CardTitle>Submit Offer</CardTitle></CardHeader>
          <CardContent>
            <form className="space-y-3" onSubmit={form.handleSubmit((value) => makeOffer.mutate(value))}>
              <Input type="number" step="0.01" {...form.register("amount", { valueAsNumber: true })} />
              <Button type="submit" disabled={makeOffer.isPending}>Create Offer</Button>
            </form>
          </CardContent>
        </Card>
      </RoleGate>
    </div>
  );
}
