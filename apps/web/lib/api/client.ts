import { z } from "zod";
import { correlationId } from "@/lib/utils/correlation";
import { ApiError, normalizeApiError } from "@/lib/api/errors";
import { notifyAuthRequired } from "@/lib/auth/session";

function isJsonResponse(response: Response) {
  return response.headers.get("content-type")?.includes("application/json");
}

export async function apiFetch(path: string, options?: RequestInit): Promise<unknown>;
export async function apiFetch<TSchema extends z.ZodTypeAny>(
  path: string,
  options: RequestInit | undefined,
  schema: TSchema
): Promise<z.output<TSchema>>;
export async function apiFetch(path: string, options: RequestInit = {}, schema?: z.ZodTypeAny): Promise<unknown> {
  const targetPath = path.startsWith("/api/") ? path : `/api/proxy/${path.replace(/^\/+/, "")}`;
  const headers = new Headers(options.headers ?? {});
  const hasBody = options.body !== undefined && options.body !== null;
  const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;

  if (!headers.has("Content-Type") && hasBody && !isFormData) {
    headers.set("Content-Type", "application/json");
  }

  headers.set("X-Correlation-Id", correlationId());

  const response = await fetch(targetPath, {
    ...options,
    headers,
    credentials: "include"
  });

  if (!response.ok) {
    const err = await normalizeApiError(response);
    if (err.status === 401) {
      notifyAuthRequired();
    }
    throw err;
  }

  if (response.status === 204) {
    return undefined;
  }

  if (!isJsonResponse(response)) {
    return undefined;
  }

  const data = (await response.json()) as unknown;
  if (!schema) {
    return data;
  }

  const parsed = schema.safeParse(data);
  if (!parsed.success) {
    throw new ApiError({
      status: 500,
      title: "Invalid API response",
      detail: parsed.error.issues.map((issue) => `${issue.path.join(".")}: ${issue.message}`).join("; "),
      problem: {
        Status: 500,
        Title: "Invalid API response",
        Detail: parsed.error.issues.map((issue) => `${issue.path.join(".")}: ${issue.message}`).join("; ")
      },
      raw: data
    });
  }

  return parsed.data;
}
