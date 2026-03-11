"use client";

import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { createProject } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/errors";

const schema = z.object({
  title: z.string().min(3),
  description: z.string().optional(),
  totalBudget: z.coerce.number().nonnegative().optional(),
  retentionRate: z.coerce.number().min(0).max(100).optional(),
  currency: z.string().default("TZS")
});

type FormData = z.infer<typeof schema>;

export default function NewProjectPage() {
  const router = useRouter();
  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: "",
      description: "",
      totalBudget: 0,
      retentionRate: 10,
      currency: "TZS"
    }
  });

  const mutation = useMutation({
    mutationFn: (data: FormData) => createProject(data),
    onSuccess: (project) => {
      toast.success("Project created");
      router.push(`/app/construction/projects/${project.id}`);
    },
    onError: (error: unknown) => {
      toast.error(error instanceof ApiError ? error.detail : "Create failed");
    }
  });

  return (
    <Card className="max-w-2xl">
      <CardHeader>
        <CardTitle>New Construction Project</CardTitle>
      </CardHeader>
      <CardContent>
        <form className="grid gap-3" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
          <Input placeholder="Project title" {...form.register("title")} />
          <Input placeholder="Description" {...form.register("description")} />
          <Input type="number" step="0.01" placeholder="Total budget" {...form.register("totalBudget", { valueAsNumber: true })} />
          <Input type="number" step="0.01" placeholder="Retention rate (%)" {...form.register("retentionRate", { valueAsNumber: true })} />
          <Input placeholder="Currency" {...form.register("currency")} />
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Saving..." : "Create Project"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
