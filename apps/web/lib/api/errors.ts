export type ApiErrorShape = {
  status: number;
  title: string;
  detail: string;
  rawDetail?: string;
  type?: string;
  instance?: string;
  extensions?: Record<string, unknown>;
  correlationId?: string;
};

export class ApiError extends Error {
  status: number;
  title: string;
  detail: string;
  rawDetail?: string;
  type?: string;
  instance?: string;
  extensions?: Record<string, unknown>;
  correlationId?: string;

  constructor(payload: ApiErrorShape) {
    super(payload.detail || payload.title);
    this.name = "ApiError";
    this.status = payload.status;
    this.title = payload.title;
    this.detail = payload.detail;
    this.rawDetail = payload.rawDetail;
    this.type = payload.type;
    this.instance = payload.instance;
    this.extensions = payload.extensions;
    this.correlationId = payload.correlationId;
  }
}

export async function normalizeApiError(response: Response): Promise<ApiError> {
  let parsed: Record<string, unknown> = {};
  try {
    parsed = (await response.json()) as Record<string, unknown>;
  } catch {
    parsed = {};
  }

  const detail =
    (typeof parsed.Detail === "string" ? parsed.Detail : undefined) ||
    (typeof parsed.detail === "string" ? parsed.detail : undefined) ||
    response.statusText ||
    "Unexpected error";

  return new ApiError({
    status: response.status,
    title: (typeof parsed.error === "string" ? parsed.error : response.statusText) || "Error",
    detail,
    rawDetail: typeof parsed.Detail === "string" ? parsed.Detail : undefined,
    type: typeof parsed.type === "string" ? parsed.type : undefined,
    instance: typeof parsed.path === "string" ? parsed.path : undefined,
    correlationId: typeof parsed.correlationId === "string" ? parsed.correlationId : undefined,
    extensions: parsed
  });
}
