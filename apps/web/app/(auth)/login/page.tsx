"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useActor } from "@/components/auth/useActor";
import { apiFetch } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";

const loginSchema = z.object({
  identifier: z.string().min(1),
  password: z.string().min(1)
});

type LoginForm = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const search = useSearchParams();
  const { isAuthenticated, refetch } = useActor();

  const form = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { identifier: "", password: "" }
  });

  useEffect(() => {
    if (isAuthenticated) {
      router.replace("/app/dashboard");
    }
  }, [isAuthenticated, router]);

  const loginMutation = useMutation({
    mutationFn: (values: LoginForm) => apiFetch("/api/auth/login", { method: "POST", body: JSON.stringify(values) }),
    onSuccess: async () => {
      await refetch();
      router.push(search.get("next") ?? "/app/dashboard");
    },
    onError: (error: unknown) => {
      const message = error instanceof ApiError ? (error.rawDetail ?? error.detail) : "Login failed";
      toast.error(message);
    }
  });

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Sign in</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={form.handleSubmit((values) => loginMutation.mutate(values))}>
            <div className="space-y-2">
              <label className="text-sm">Email or Phone</label>
              <Input type="text" {...form.register("identifier")} />
              {form.formState.errors.identifier && <p className="text-xs text-red-600">{form.formState.errors.identifier.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm">Password</label>
              <Input type="password" {...form.register("password")} />
              {form.formState.errors.password && <p className="text-xs text-red-600">{form.formState.errors.password.message}</p>}
            </div>
            <Button className="w-full" type="submit" disabled={loginMutation.isPending}>
              {loginMutation.isPending ? "Signing in..." : "Login"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
