"use client";

import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { createProperty } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/errors";

const schema = z.object({
  title: z.string().min(3),
  description: z.string().optional(),
  location: z.string().min(2),
  askingPrice: z.coerce.number().positive(),
  currency: z.string().default("TZS"),
  propertyType: z.string().default("HOUSE"),
  bedrooms: z.coerce.number().int().min(0)
});

type FormData = z.infer<typeof schema>;

export default function NewPropertyPage() {
  const router = useRouter();
  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: "",
      description: "",
      location: "",
      askingPrice: 0,
      currency: "TZS",
      propertyType: "HOUSE",
      bedrooms: 0
    }
  });

  const mutation = useMutation({
    mutationFn: (data: FormData) => createProperty(data),
    onSuccess: (property) => {
      toast.success("Property created");
      router.push(`/app/marketplace/properties/${property.id}`);
    },
    onError: (error: unknown) => {
      const message = error instanceof ApiError ? error.detail : "Create failed";
      toast.error(message);
    }
  });

  return (
    <Card className="max-w-2xl">
      <CardHeader>
        <CardTitle>New Property</CardTitle>
      </CardHeader>
      <CardContent>
        <form className="grid gap-3" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
          <Input placeholder="Title" {...form.register("title")} />
          <Input placeholder="Location" {...form.register("location")} />
          <Input placeholder="Description" {...form.register("description")} />
          <Input type="number" step="0.01" placeholder="Price" {...form.register("askingPrice", { valueAsNumber: true })} />
          <Input placeholder="Property Type" {...form.register("propertyType")} />
          <Input type="number" placeholder="Bedrooms" {...form.register("bedrooms", { valueAsNumber: true })} />
          <Button type="submit" disabled={mutation.isPending}>{mutation.isPending ? "Saving..." : "Create"}</Button>
        </form>
      </CardContent>
    </Card>
  );
}
