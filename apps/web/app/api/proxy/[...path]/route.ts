import { NextRequest, NextResponse } from "next/server";
import { authCookieOptions, getAuthCookieName } from "@/lib/auth/session";

const backendUrl = process.env.NEXT_PUBLIC_BACKEND_BASE_URL ?? "http://localhost:8080";

async function handle(request: NextRequest, params: { path: string[] }) {
  const authCookieName = getAuthCookieName();
  const token = request.cookies.get(authCookieName)?.value;
  const path = params.path.join("/");
  const target = new URL(`${backendUrl}/${path}`);
  request.nextUrl.searchParams.forEach((value, key) => target.searchParams.set(key, value));

  const headers = new Headers();
  headers.set("x-correlation-id", request.headers.get("x-correlation-id") ?? crypto.randomUUID());
  if (token) {
    headers.set("authorization", `Bearer ${token}`);
  }

  const contentType = request.headers.get("content-type");
  if (contentType) {
    headers.set("content-type", contentType);
  }

  const method = request.method.toUpperCase();
  const body = method === "GET" || method === "HEAD" ? undefined : await request.text();

  const response = await fetch(target.toString(), {
    method,
    headers,
    body,
    cache: "no-store",
    credentials: "include"
  });

  const text = await response.text();
  const nextResponse = new NextResponse(text, {
    status: response.status,
    headers: {
      "content-type": response.headers.get("content-type") ?? "application/json",
      "x-correlation-id": headers.get("x-correlation-id") ?? ""
    }
  });

  if (response.status === 401) {
    nextResponse.cookies.set(authCookieName, "", authCookieOptions(0));
  }

  return nextResponse;
}

export async function GET(request: NextRequest, { params }: { params: { path: string[] } }) {
  return handle(request, params);
}

export async function POST(request: NextRequest, { params }: { params: { path: string[] } }) {
  return handle(request, params);
}

export async function PUT(request: NextRequest, { params }: { params: { path: string[] } }) {
  return handle(request, params);
}

export async function PATCH(request: NextRequest, { params }: { params: { path: string[] } }) {
  return handle(request, params);
}

export async function DELETE(request: NextRequest, { params }: { params: { path: string[] } }) {
  return handle(request, params);
}
