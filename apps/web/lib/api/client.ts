import { ZodSchema } from "zod";
import { correlationId } from "@/lib/utils/correlation";
import { ApiError, normalizeApiError } from "@/lib/api/errors";

function isJsonResponse(response: Response) {
  return response.headers.get("content-type")?.includes("application/json");
}

export async function apiFetch<T>(path: string, options: RequestInit = {}, schema?: ZodSchema<T>): Promise<T> {
  const headers = new Headers(options.headers ?? {});
  const hasBody = options.body !== undefined && options.body !== null;
  const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;

  if (!headers.has("Content-Type") && hasBody && !isFormData) {
    headers.set("Content-Type", "application/json");
  }

  headers.set("X-Correlation-Id", correlationId());

  const response = await fetch(path, {
    ...options,
    headers,
    credentials: "include"
  });

  if (!response.ok) {
    const err = await normalizeApiError(response);
    if (typeof window !== "undefined" && err.status === 401 && window.location.pathname !== "/login") {
      window.location.href = "/login";
    }
    throw err;
  }

  if (response.status === 204) {
    return undefined as T;
  }

  if (!isJsonResponse(response)) {
    return undefined as T;
  }

  const data = (await response.json()) as unknown;
  if (!schema) {
    return data as T;
  }

  const parsed = schema.safeParse(data);
  if (!parsed.success) {
    throw new ApiError({
      status: 500,
      title: "Invalid API response",
      detail: parsed.error.issues.map((issue) => `${issue.path.join(".")}: ${issue.message}`).join("; ")
    });
  }

  return parsed.data;
}
