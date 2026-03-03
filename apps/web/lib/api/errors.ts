export type ApiProblem = {
  Type?: string;
  Title?: string;
  Status: number;
  Detail?: string;
  Instance?: string;
  Extensions?: Record<string, unknown>;
};

export type ApiErrorShape = {
  status: number;
  title: string;
  detail: string;
  problem?: ApiProblem;
  raw?: unknown;
  correlationId?: string;
};

export class ApiError extends Error {
  status: number;
  title: string;
  detail: string;
  problem?: ApiProblem;
  raw: unknown;
  correlationId?: string;

  constructor(payload: ApiErrorShape) {
    super(payload.detail || payload.title);
    this.name = "ApiError";
    this.status = payload.status;
    this.title = payload.title;
    this.detail = payload.detail;
    this.problem = payload.problem;
    this.raw = payload.raw;
    this.correlationId = payload.correlationId;
  }
}

function asRecord(value: unknown) {
  return value && typeof value === "object" ? (value as Record<string, unknown>) : undefined;
}

function getString(record: Record<string, unknown> | undefined, ...keys: string[]) {
  if (!record) {
    return undefined;
  }

  for (const key of keys) {
    if (typeof record[key] === "string") {
      return record[key] as string;
    }
  }

  return undefined;
}

export async function parseErrorPayload(response: Response) {
  const contentType = response.headers.get("content-type") ?? "";
  const rawText = await response.text();

  if (!rawText) {
    return { raw: undefined, problem: undefined };
  }

  if (contentType.includes("application/json")) {
    try {
      const raw = JSON.parse(rawText) as unknown;
      const record = asRecord(raw);
      const problem: ApiProblem = {
        Type: getString(record, "Type", "type"),
        Title: getString(record, "Title", "title", "error"),
        Status: typeof record?.Status === "number"
          ? record.Status
          : typeof record?.status === "number"
            ? record.status
            : response.status,
        Detail: getString(record, "Detail", "detail", "message"),
        Instance: getString(record, "Instance", "instance", "path"),
        Extensions: record
      };
      return { raw, problem };
    } catch {
      return { raw: rawText, problem: undefined };
    }
  }

  return {
    raw: rawText,
    problem: {
      Status: response.status,
      Detail: rawText
    }
  };
}

export async function normalizeApiError(response: Response): Promise<ApiError> {
  const { raw, problem } = await parseErrorPayload(response);
  const detail = problem?.Detail || response.statusText || "Unexpected error";
  const title = problem?.Title || response.statusText || "Error";
  const correlationId = getString(asRecord(raw), "correlationId", "CorrelationId");

  return new ApiError({
    status: response.status,
    title,
    detail,
    problem,
    raw,
    correlationId
  });
}
