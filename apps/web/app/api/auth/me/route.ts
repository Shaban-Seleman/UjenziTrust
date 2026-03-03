import { NextRequest, NextResponse } from "next/server";
import { actorSchema } from "@/lib/api/schemas";
import { authCookieOptions, getAuthCookieName } from "@/lib/auth/session";

const backendUrl = process.env.NEXT_PUBLIC_BACKEND_BASE_URL ?? "http://localhost:8080";

export async function GET(request: NextRequest) {
  const authCookieName = getAuthCookieName();
  const token = request.cookies.get(authCookieName)?.value;
  if (!token) {
    return NextResponse.json(
      {
        Type: "about:blank",
        Title: "Unauthorized",
        Status: 401,
        Detail: "Authentication required"
      },
      { status: 401 }
    );
  }

  const correlationId = request.headers.get("x-correlation-id") ?? crypto.randomUUID();

  const response = await fetch(`${backendUrl}/auth/me`, {
    method: "GET",
    headers: {
      authorization: `Bearer ${token}`,
      "x-correlation-id": correlationId
    },
    cache: "no-store",
    credentials: "include"
  });

  const raw = await response.text();
  const contentType = response.headers.get("content-type") ?? "application/json";

  if (response.ok) {
    try {
      const parsed = JSON.parse(raw) as unknown;
      const validated = actorSchema.safeParse(parsed);
      if (!validated.success) {
        return NextResponse.json(
          {
            Type: "about:blank",
            Title: "Invalid actor payload",
            Status: 502,
            Detail: "Backend actor response did not match the expected schema"
          },
          {
            status: 502,
            headers: {
              "x-correlation-id": correlationId
            }
          }
        );
      }

      return NextResponse.json(validated.data, {
        status: 200,
        headers: {
          "x-correlation-id": correlationId
        }
      });
    } catch {
      return NextResponse.json(
        {
          Type: "about:blank",
          Title: "Invalid actor payload",
          Status: 502,
          Detail: "Backend actor response was not valid JSON"
        },
        {
          status: 502,
          headers: {
            "x-correlation-id": correlationId
          }
        }
      );
    }
  }

  const nextResponse = new NextResponse(raw, {
    status: response.status,
    headers: {
      "content-type": contentType,
      "x-correlation-id": correlationId
    }
  });

  if (response.status === 401) {
    nextResponse.cookies.set(authCookieName, "", authCookieOptions(0));
  }

  return nextResponse;
}
