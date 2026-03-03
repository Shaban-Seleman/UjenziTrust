import { LoginPageClient } from "@/components/auth/LoginPageClient";

export default function LoginPage({
  searchParams
}: {
  searchParams?: { next?: string };
}) {
  return <LoginPageClient nextHref={searchParams?.next} />;
}
