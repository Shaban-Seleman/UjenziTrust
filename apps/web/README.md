# NyumbaTrust Frontend (apps/web)

Next.js App Router dashboard for Tanzania Real Estate + Escrow + Construction workflows.

## Stack

- Next.js 14 + TypeScript
- Tailwind CSS
- shadcn-style UI components
- TanStack Query
- TanStack Table
- React Hook Form + Zod

## Environment

Copy `.env.local.example` to `.env.local`.

```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_APP_NAME=NyumbaTrust
```

## Run

```bash
cd apps/web
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Auth model

- `POST /api/auth/login` proxies backend `/auth/login`.
- If backend returns `accessToken`, Next route sets HttpOnly `access_token` cookie.
- All dashboard API requests use `/api/proxy/*`, which injects `Authorization: Bearer <cookie-token>` server-side.
- `GET /api/auth/me` resolves actor session from backend `/auth/me`.

## Route protection

- Middleware protects `/app/*` by checking `access_token` cookie.
- `RequireAuth` validates actor session on the client and redirects to `/login`.
- Role guards are enforced in nav visibility and page access checks.

## Notes

Some dashboard panels call optional backend read endpoints (offers/reservations/escrows/projects/admin streams). If these endpoints are not implemented yet in backend, pages show an unavailable message while the rest of the app remains functional.
