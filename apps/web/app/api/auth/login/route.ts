import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const backendUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export async function POST(request: NextRequest) {
  const body = await request.text();
  const correlationId = request.headers.get("x-correlation-id") ?? crypto.randomUUID();

  const response = await fetch(`${backendUrl}/auth/login`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-correlation-id": correlationId
    },
    body,
    cache: "no-store"
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
  if (token) {
    cookies().set("access_token", token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/",
      maxAge: 60 * 60 * 12
    });
  }
  const nextResponse = NextResponse.json({ ok: true }, { headers: { "x-correlation-id": correlationId } });
  const backendSetCookie = response.headers.get("set-cookie");
  if (backendSetCookie) {
    nextResponse.headers.set("set-cookie", backendSetCookie);
  }
  return nextResponse;
}
