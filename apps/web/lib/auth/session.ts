export const ACCESS_TOKEN_COOKIE = "nt_access_token";
export const AUTH_REQUIRED_EVENT = "uzenji:auth-required";

export function getAuthCookieName() {
  return process.env.AUTH_COOKIE_NAME ?? ACCESS_TOKEN_COOKIE;
}

export function authCookieOptions(maxAge?: number) {
  return {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax" as const,
    path: "/",
    ...(maxAge === undefined ? {} : { maxAge })
  };
}

export function buildLoginHref(nextPath?: string) {
  if (!nextPath || nextPath === "/login") {
    return "/login";
  }

  return `/login?next=${encodeURIComponent(nextPath)}`;
}

export function notifyAuthRequired() {
  if (typeof window === "undefined") {
    return;
  }

  window.dispatchEvent(new CustomEvent(AUTH_REQUIRED_EVENT));
}
