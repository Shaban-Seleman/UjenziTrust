"use client";

import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { RoleGate } from "@/components/auth/RoleGate";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { getProperty, publishProperty, submitOffer, updateProperty } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/errors";
import { formatMoney } from "@/lib/utils/money";

const offerSchema = z.object({
  amount: z.coerce.number().positive("Offer amount must be positive")
});

const propertyEditSchema = z.object({
  title: z.string().min(3, "Title is required"),
  description: z.string().optional(),
  location: z.string().min(2, "Location is required"),
  askingPrice: z.coerce.number().positive("Asking price must be positive"),
  currency: z.string().min(3, "Currency is required")
});

type OfferForm = z.infer<typeof offerSchema>;
type PropertyEditForm = z.infer<typeof propertyEditSchema>;

function errorDetail(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.detail : fallback;
}

export default function PropertyDetailPage({ params }: { params: { id: string } }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [showEdit, setShowEdit] = useState(false);
  const propertyQuery = useQuery({ queryKey: ["property", params.id], queryFn: () => getProperty(params.id) });

  const offerForm = useForm<OfferForm>({
    resolver: zodResolver(offerSchema),
    defaultValues: { amount: 0 }
  });

  const editForm = useForm<PropertyEditForm>({
    resolver: zodResolver(propertyEditSchema),
    defaultValues: {
      title: "",
      description: "",
      location: "",
      askingPrice: 0,
      currency: "TZS"
    }
  });

  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["properties"] }),
      queryClient.invalidateQueries({ queryKey: ["my-properties"] }),
      queryClient.invalidateQueries({ queryKey: ["property", params.id] })
    ]);
  };

  const publish = useMutation({
    mutationFn: () => publishProperty(params.id),
    onSuccess: async () => {
      await refresh();
      toast.success("Property published");
      router.push("/app/marketplace/properties");
    },
    onError: (error) => toast.error(errorDetail(error, "Publish failed"))
  });

  const saveEdit = useMutation({
    mutationFn: (data: PropertyEditForm) => updateProperty(params.id, data),
    onSuccess: async (property) => {
      editForm.reset({
        title: property.title,
        description: property.description ?? "",
        location: property.location ?? "",
        askingPrice: property.askingPrice ?? 0,
        currency: property.currency ?? "TZS"
      });
      setShowEdit(false);
      await refresh();
      toast.success("Property updated");
    },
    onError: (error) => toast.error(errorDetail(error, "Update failed"))
  });

  const makeOffer = useMutation({
    mutationFn: (data: OfferForm) => submitOffer(params.id, { amount: data.amount, currency: "TZS" }),
    onSuccess: () => {
      offerForm.reset({ amount: 0 });
      toast.success("Offer submitted");
    },
    onError: (error) => toast.error(errorDetail(error, "Offer submission failed"))
  });

  const property = propertyQuery.data;
  const editDisabledReason = useMemo(() => {
    if (!property) {
      return null;
    }
    if (!["DRAFT", "PUBLISHED"].includes(property.status)) {
      return `Editing is disabled while property is ${property.status}.`;
    }
    return null;
  }, [property]);

  if (propertyQuery.isLoading) {
    return <p className="text-sm text-muted-foreground">Loading property...</p>;
  }

  if (propertyQuery.error || !property) {
    return <p className="text-sm text-red-700">{errorDetail(propertyQuery.error, "Property unavailable.")}</p>;
  }

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="gap-3 md:flex md:flex-row md:items-start md:justify-between">
          <div>
            <CardTitle>{property.title}</CardTitle>
            <p className="mt-2 text-sm text-muted-foreground">Status: {property.status}</p>
          </div>
          <RoleGate roles={["SELLER", "OWNER", "ADMIN"]}>
            <Dialog
              open={showEdit}
              onOpenChange={(open) => {
                setShowEdit(open);
                if (open) {
                  editForm.reset({
                    title: property.title,
                    description: property.description ?? "",
                    location: property.location ?? "",
                    askingPrice: property.askingPrice ?? 0,
                    currency: property.currency ?? "TZS"
                  });
                }
              }}
            >
              <DialogTrigger asChild>
                <Button variant="outline" disabled={Boolean(editDisabledReason)}>
                  Edit Property
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Edit Property</DialogTitle>
                </DialogHeader>
                {editDisabledReason ? (
                  <p className="text-sm text-muted-foreground">{editDisabledReason}</p>
                ) : (
                  <form className="space-y-3" onSubmit={editForm.handleSubmit((values) => saveEdit.mutate(values))}>
                    <div className="space-y-1">
                      <label className="text-sm font-medium" htmlFor="title">Title</label>
                      <Input id="title" {...editForm.register("title")} />
                      {editForm.formState.errors.title ? <p className="text-xs text-red-700">{editForm.formState.errors.title.message}</p> : null}
                    </div>
                    <div className="space-y-1">
                      <label className="text-sm font-medium" htmlFor="location">Location</label>
                      <Input id="location" {...editForm.register("location")} />
                      {editForm.formState.errors.location ? <p className="text-xs text-red-700">{editForm.formState.errors.location.message}</p> : null}
                    </div>
                    <div className="space-y-1">
                      <label className="text-sm font-medium" htmlFor="description">Description</label>
                      <Textarea id="description" {...editForm.register("description")} />
                    </div>
                    <div className="grid gap-3 md:grid-cols-2">
                      <div className="space-y-1">
                        <label className="text-sm font-medium" htmlFor="askingPrice">Asking Price</label>
                        <Input id="askingPrice" type="number" step="0.01" {...editForm.register("askingPrice", { valueAsNumber: true })} />
                        {editForm.formState.errors.askingPrice ? <p className="text-xs text-red-700">{editForm.formState.errors.askingPrice.message}</p> : null}
                      </div>
                      <div className="space-y-1">
                        <label className="text-sm font-medium" htmlFor="currency">Currency</label>
                        <Input id="currency" {...editForm.register("currency")} />
                        {editForm.formState.errors.currency ? <p className="text-xs text-red-700">{editForm.formState.errors.currency.message}</p> : null}
                      </div>
                    </div>
                    <Button type="submit" disabled={saveEdit.isPending}>
                      {saveEdit.isPending ? "Saving..." : "Save Changes"}
                    </Button>
                  </form>
                )}
              </DialogContent>
            </Dialog>
          </RoleGate>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <p>{property.description || "No description provided."}</p>
          <p>Location: {property.location ?? "-"}</p>
          <p>Price: {formatMoney(property.askingPrice ?? 0, property.currency ?? "TZS")}</p>
          <p>Currency: {property.currency ?? "TZS"}</p>
          {editDisabledReason ? <p className="text-xs text-muted-foreground">{editDisabledReason}</p> : null}
          <RoleGate roles={["SELLER", "OWNER", "ADMIN"]}>
            <Button
              onClick={() => publish.mutate()}
              disabled={publish.isPending || property.status === "PUBLISHED"}
            >
              {property.status === "PUBLISHED" ? "Published" : publish.isPending ? "Publishing..." : "Publish"}
            </Button>
          </RoleGate>
        </CardContent>
      </Card>

      <RoleGate roles={["BUYER"]}>
        <Card className="max-w-md">
          <CardHeader><CardTitle>Submit Offer</CardTitle></CardHeader>
          <CardContent>
            <form className="space-y-3" onSubmit={offerForm.handleSubmit((value) => makeOffer.mutate(value))}>
              <Input type="number" step="0.01" {...offerForm.register("amount", { valueAsNumber: true })} />
              {offerForm.formState.errors.amount ? <p className="text-xs text-red-700">{offerForm.formState.errors.amount.message}</p> : null}
              <Button type="submit" disabled={makeOffer.isPending}>
                {makeOffer.isPending ? "Submitting..." : "Create Offer"}
              </Button>
            </form>
          </CardContent>
        </Card>
      </RoleGate>
    </div>
  );
}
