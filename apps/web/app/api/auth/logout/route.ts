import { NextResponse } from "next/server";
import { authCookieOptions, getAuthCookieName } from "@/lib/auth/session";

export async function POST() {
  const response = NextResponse.json({ ok: true });
  response.cookies.set(getAuthCookieName(), "", authCookieOptions(0));

  return response;
}
