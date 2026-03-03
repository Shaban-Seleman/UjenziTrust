# NyumbaTrust Frontend

Package 1 provides the secured frontend foundation for the NyumbaTrust web app.

## Stack

- Next.js 14 App Router + TypeScript
- Tailwind CSS
- shadcn/ui components
- TanStack Query
- Zod
- React Hook Form

## Install

```bash
cd apps/web
npm install
```

## Environment

Copy [`.env.local.example`](/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/.env.local.example) to `.env.local`.

```bash
NEXT_PUBLIC_APP_NAME=NyumbaTrust
NEXT_PUBLIC_BACKEND_BASE_URL=http://localhost:8080
AUTH_COOKIE_NAME=nt_access_token
```

`NEXT_PUBLIC_BACKEND_BASE_URL` is consumed only inside Next.js route handlers. Browser code talks only to Next.js routes under `/api/*`.

## Run

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Auth Model

- The Spring Boot backend returns a JWT/access token in JSON from `POST /auth/login`.
- `POST /api/auth/login` in Next.js proxies that request, extracts `accessToken`, and stores it in an HttpOnly cookie.
- The token is never returned to browser JavaScript and is never stored in `localStorage` or `sessionStorage`.
- `GET /api/auth/me` and `/api/proxy/*` read the HttpOnly cookie on the server and inject `Authorization: Bearer <token>` when calling the backend.
- If the backend returns `401`, the proxy returns `401` and clears the auth cookie.

## Protected Routes

- `/dashboard`
- `/marketplace`
- `/escrows`
- `/construction`
- `/admin`

These routes render inside the protected app shell and use `RequireAuth` plus role-aware navigation and page-level `RoleGate` checks.

## Structure

- `app/api/auth/*`: login, logout, actor session
- `app/api/proxy/[...path]`: generic backend proxy
- `components/layout/*`: app shell, sidebar, topbar
- `components/auth/*`: auth hooks and guards
- `lib/api/*`: API client, typed wrappers, schemas, error normalization
- `lib/auth/*`: roles, session helpers, route guards

## Notes

Package 1 intentionally ships placeholder module pages only. Marketplace, escrow, construction, and admin feature UIs are deferred to later packages.
