import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const backendUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export async function GET(request: NextRequest) {
  const token = cookies().get("access_token")?.value;
  if (!token) {
    return NextResponse.json({ error: "Unauthorized", Detail: "Authentication required" }, { status: 401 });
  }

  const correlationId = request.headers.get("x-correlation-id") ?? crypto.randomUUID();

  const response = await fetch(`${backendUrl}/auth/me`, {
    method: "GET",
    headers: {
      authorization: `Bearer ${token}`,
      "x-correlation-id": correlationId
    },
    cache: "no-store"
  });

  const raw = await response.text();
  const contentType = response.headers.get("content-type") ?? "application/json";

  return new NextResponse(raw, {
    status: response.status,
    headers: {
      "content-type": contentType,
      "x-correlation-id": correlationId
    }
  });
}
