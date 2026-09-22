import { AuthSession } from "./auth";

/** Advanced mode or platform operator — full analytics, family tools, dashboards. */
export function hasAdvancedAccess(session: AuthSession | null | undefined): boolean {
  return session?.uiMode === "advanced" || session?.platformAdmin === true;
}

export function isPlatformAdmin(session: AuthSession | null | undefined): boolean {
  return session?.platformAdmin === true;
}
