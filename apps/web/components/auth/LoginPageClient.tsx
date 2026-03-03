"use client";

import { useEffect } from "react";
import type { Route } from "next";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useActor } from "@/components/auth/useActor";
import { login } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/errors";

const loginSchema = z.object({
  email: z.string().email("Enter a valid email address"),
  password: z.string().min(1)
});

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPageClient({ nextHref }: { nextHref?: string }) {
  const router = useRouter();
  const { isAuthenticated, refetch } = useActor();

  const form = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" }
  });

  useEffect(() => {
    if (isAuthenticated) {
      router.replace((nextHref ?? "/dashboard") as Route);
    }
  }, [isAuthenticated, nextHref, router]);

  const loginMutation = useMutation({
    mutationFn: (values: LoginForm) => login(values.email, values.password),
    onSuccess: async () => {
      await refetch();
      router.push((nextHref ?? "/dashboard") as Route);
    },
    onError: () => undefined
  });

  const errorMessage = loginMutation.error instanceof ApiError ? loginMutation.error.detail : null;

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Sign in</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={form.handleSubmit((values) => loginMutation.mutate(values))}>
            <div className="space-y-2">
              <label className="text-sm">Email</label>
              <Input type="email" autoComplete="email" {...form.register("email")} />
              {form.formState.errors.email && <p className="text-xs text-red-600">{form.formState.errors.email.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm">Password</label>
              <Input type="password" autoComplete="current-password" {...form.register("password")} />
              {form.formState.errors.password && <p className="text-xs text-red-600">{form.formState.errors.password.message}</p>}
            </div>
            {errorMessage ? <p className="text-sm text-red-600">{errorMessage}</p> : null}
            <Button className="w-full" type="submit" disabled={loginMutation.isPending}>
              {loginMutation.isPending ? "Signing in..." : "Login"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
