import { Slot, usePathname } from "expo-router";
import { AppShell } from "../../components/AppShell";
import { FamilyProvider, useFamily } from "../../lib/FamilyContext";

function titleForPath(pathname: string): string {
  if (pathname.includes("settings")) return "Settings";
  if (pathname.includes("features")) return "What's available";
  if (pathname.includes("reports")) return "Reports";
  if (pathname.includes("report/")) return "Report detail";
  if (pathname.includes("family")) return "Family";
  if (pathname.includes("trends")) return "Trends";
  if (pathname.includes("dashboard")) return "Dashboard";
  if (pathname.includes("ask")) return "Ask your data";
  if (pathname.includes("imaging/")) return "Imaging study";
  if (pathname.includes("ops-pipeline")) return "Pipeline trace";
  if (pathname.includes("ops")) return "Ops monitoring";
  if (pathname.includes("feedback")) return "Send feedback";
  return "Home";
}

function AppLayoutInner() {
  const pathname = usePathname();
  const { activeFamily } = useFamily();

  return (
    <AppShell title={titleForPath(pathname)}>
      <Slot key={activeFamily?.id ?? "default"} />
    </AppShell>
  );
}

export default function AppLayout() {
  return (
    <FamilyProvider>
      <AppLayoutInner />
    </FamilyProvider>
  );
}
