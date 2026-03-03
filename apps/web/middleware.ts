import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { buildLoginHref, getAuthCookieName } from "@/lib/auth/session";

export function middleware(request: NextRequest) {
  const protectedPrefixes = ["/dashboard", "/marketplace", "/escrows", "/construction", "/admin", "/app"];
  if (!protectedPrefixes.some((prefix) => request.nextUrl.pathname.startsWith(prefix))) {
    return NextResponse.next();
  }

  const token = request.cookies.get(getAuthCookieName())?.value;
  if (!token) {
    const nextPath = `${request.nextUrl.pathname}${request.nextUrl.search}`;
    const loginUrl = new URL(buildLoginHref(nextPath), request.url);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/dashboard/:path*", "/marketplace/:path*", "/escrows/:path*", "/construction/:path*", "/admin/:path*", "/app/:path*"]
};
