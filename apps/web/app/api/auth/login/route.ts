import { NextRequest, NextResponse } from "next/server";
import { authCookieOptions, getAuthCookieName } from "@/lib/auth/session";

const backendUrl = process.env.NEXT_PUBLIC_BACKEND_BASE_URL ?? "http://localhost:8080";

function resolveMaxAge(expiresAt: unknown) {
  if (typeof expiresAt !== "string") {
    return 60 * 60 * 12;
  }

  const expiry = new Date(expiresAt).getTime();
  if (Number.isNaN(expiry)) {
    return 60 * 60 * 12;
  }

  const seconds = Math.floor((expiry - Date.now()) / 1000);
  return seconds > 0 ? seconds : 60 * 60 * 12;
}

export async function POST(request: NextRequest) {
  const payload = (await request.json().catch(() => null)) as
    | { email?: string; password?: string; identifier?: string }
    | null;
  const body = JSON.stringify({
    identifier: payload?.email ?? payload?.identifier ?? "",
    password: payload?.password ?? ""
  });
  const correlationId = request.headers.get("x-correlation-id") ?? crypto.randomUUID();

  const response = await fetch(`${backendUrl}/auth/login`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-correlation-id": correlationId
    },
    body,
    cache: "no-store",
    credentials: "include"
  });

  const contentType = response.headers.get("content-type") ?? "application/json";
  const raw = await response.text();

  if (!response.ok) {
    return new NextResponse(raw, {
      status: response.status,
      headers: {
        "content-type": contentType,
        "x-correlation-id": correlationId
      }
    });
  }

  let parsed: Record<string, unknown> = {};
  try {
    parsed = JSON.parse(raw) as Record<string, unknown>;
  } catch {
    parsed = {};
  }

  const token = typeof parsed.accessToken === "string" ? parsed.accessToken : undefined;
  if (!token) {
    return NextResponse.json(
      {
        Type: "about:blank",
        Title: "Invalid auth response",
        Status: 502,
        Detail: "Backend login response did not include an access token"
      },
      { status: 502, headers: { "x-correlation-id": correlationId } }
    );
  }

  const nextResponse = NextResponse.json(
    {
      ok: true,
      userId: parsed.userId,
      roles: parsed.roles,
      expiresAt: parsed.expiresAt
    },
    { headers: { "x-correlation-id": correlationId } }
  );
  nextResponse.cookies.set(getAuthCookieName(), token, {
    ...authCookieOptions(resolveMaxAge(parsed.expiresAt))
  });

  return nextResponse;
}
