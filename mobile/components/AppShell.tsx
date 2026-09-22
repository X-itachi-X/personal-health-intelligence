import { router, usePathname } from "expo-router";
import { ReactNode, useEffect, useRef, useState } from "react";
import { Animated, Modal, Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { clearActiveFamilyId } from "../lib/activeFamily";
import { clearSession, getSession, AuthSession } from "../lib/auth";
import { hasAdvancedAccess, isPlatformAdmin } from "../lib/session";
import { useFamily } from "../lib/FamilyContext";
import { IconName, navIcons } from "../lib/icons";
import { fadeIn } from "../lib/motion";
import { useTheme } from "../lib/ThemeContext";
import { useThemedStyles } from "../lib/useThemedStyles";
import { animation, radii, spacing } from "../lib/theme";
import { AnimatedPressable } from "./ui/AnimatedPressable";
import { Icon } from "./ui/Icon";
import { MatteBackground } from "./ui/MatteBackground";
import { MatteSurface } from "./ui/MatteSurface";

type MenuItem = {
  label: string;
  route: string;
  iconKey: keyof typeof navIcons;
  advancedOnly?: boolean;
  platformAdminOnly?: boolean;
};

const BASE_MENU: MenuItem[] = [
  { label: "Home", route: "/(app)", iconKey: "home" },
  { label: "Reports", route: "/(app)/reports", iconKey: "reports" },
  { label: "Family", route: "/(app)/family", iconKey: "family", advancedOnly: true },
  { label: "Trends", route: "/(app)/trends", iconKey: "trends", advancedOnly: true },
  { label: "Dashboard", route: "/(app)/dashboard", iconKey: "dashboard", advancedOnly: true },
  { label: "Ask", route: "/(app)/ask", iconKey: "ask", advancedOnly: true },
  { label: "Platform Ops", route: "/(app)/ops", iconKey: "ops", platformAdminOnly: true },
  { label: "Send feedback", route: "/(app)/feedback", iconKey: "feedback" },
  { label: "Settings", route: "/(app)/settings", iconKey: "settings" },
  { label: "What's available", route: "/(app)/features", iconKey: "features" },
];

type AppShellProps = {
  children: ReactNode;
  title: string;
};

function isActiveRoute(pathname: string, route: string): boolean {
  if (route === "/(app)") {
    return pathname === "/" || pathname === "/(app)" || pathname.endsWith("/index");
  }
  const segment = route.replace("/(app)/", "");
  return pathname.includes(segment);
}

export function AppShell({ children, title }: AppShellProps) {
  const insets = useSafeAreaInsets();
  const pathname = usePathname();
  const { colors } = useTheme();
  const styles = useThemedStyles(({ colors, typography }) =>
    StyleSheet.create({
      headerWrap: { borderBottomWidth: 1, borderBottomColor: colors.borderLight },
      header: {
        flexDirection: "row",
        alignItems: "center",
        paddingHorizontal: spacing.md,
        paddingVertical: spacing.sm,
        gap: spacing.sm,
      },
      menuButton: {
        width: 44,
        height: 44,
        borderRadius: radii.md,
        backgroundColor: colors.surfaceMuted,
        borderWidth: 1,
        borderColor: colors.border,
        alignItems: "center",
        justifyContent: "center",
      },
      headerText: { flex: 1 },
      headerTitle: { ...typography.subtitle, fontSize: 18 },
      headerSubtitle: { ...typography.caption, marginTop: 2 },
      headerBadge: {
        backgroundColor: colors.primaryLight,
        paddingHorizontal: 10,
        paddingVertical: 6,
        borderRadius: radii.sm,
      },
      headerBadgeAdvanced: { backgroundColor: colors.adminBadge },
      headerBadgeText: { color: colors.primaryDark, fontWeight: "700", fontSize: 12 },
      content: { flex: 1 },
      modalRoot: { flex: 1, flexDirection: "row" },
      backdrop: { ...StyleSheet.absoluteFill, backgroundColor: colors.overlay },
      drawerOuter: {
        position: "absolute",
        left: 0,
        top: 0,
        bottom: 0,
        width: "82%",
        maxWidth: 320,
      },
      drawerGlass: { flex: 1, borderRightWidth: 1, borderRightColor: colors.border },
      drawer: { flex: 1, paddingHorizontal: spacing.lg },
      drawerBrandRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: spacing.lg },
      drawerBrand: { fontSize: 13, fontWeight: "600", color: colors.primaryDark, flex: 1 },
      drawerUser: {
        flexDirection: "row",
        alignItems: "center",
        gap: spacing.md,
        backgroundColor: colors.surfaceMuted,
        borderRadius: radii.md,
        padding: spacing.md,
        marginBottom: spacing.lg,
      },
      drawerAvatar: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: colors.primary,
        alignItems: "center",
        justifyContent: "center",
      },
      drawerAvatarText: { fontSize: 18, fontWeight: "700", color: colors.white },
      drawerUserInfo: { flex: 1 },
      drawerUserName: { fontSize: 16, fontWeight: "700", color: colors.text },
      drawerUserEmail: { fontSize: 12, color: colors.textMuted, marginTop: 2 },
      drawerFamily: { fontSize: 12, color: colors.primaryDark, marginTop: 4, fontWeight: "700" },
      menuList: { gap: spacing.xs, flex: 1 },
      menuItem: {
        flexDirection: "row",
        alignItems: "center",
        gap: spacing.md,
        paddingVertical: 14,
        paddingHorizontal: spacing.md,
        borderRadius: radii.md,
      },
      menuItemActive: { backgroundColor: colors.primaryLight },
      menuItemLabel: { fontSize: 16, color: colors.textMuted, fontWeight: "500" },
      menuItemLabelActive: { color: colors.primaryDark, fontWeight: "700" },
      drawerFooter: { borderTopWidth: 1, borderTopColor: colors.border, paddingTop: spacing.md },
      signOutButton: {
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "center",
        gap: spacing.sm,
        backgroundColor: colors.dangerLight,
        borderRadius: radii.md,
        paddingVertical: 14,
      },
      signOutText: { color: colors.danger, fontWeight: "600", fontSize: 16 },
    })
  );

  const [open, setOpen] = useState(false);
  const [session, setSession] = useState<AuthSession | null>(null);
  const { activeFamily } = useFamily();
  const backdropOpacity = useRef(new Animated.Value(0)).current;
  const drawerSlide = useRef(new Animated.Value(-320)).current;

  useEffect(() => {
    getSession().then(setSession);
  }, [pathname]);

  useEffect(() => {
    if (open) {
      Animated.parallel([
        fadeIn(backdropOpacity, animation.normal),
        Animated.spring(drawerSlide, { toValue: 0, friction: 8, tension: 65, useNativeDriver: true }),
      ]).start();
    } else {
      backdropOpacity.setValue(0);
      drawerSlide.setValue(-320);
    }
  }, [open, backdropOpacity, drawerSlide]);

  const menuItems = BASE_MENU.filter((item) => {
    if (item.platformAdminOnly) return session?.platformAdmin === true;
    if (item.advancedOnly) return hasAdvancedAccess(session);
    return true;
  });

  async function handleSignOut() {
    setOpen(false);
    await clearActiveFamilyId();
    await clearSession();
    router.replace("/login");
  }

  function navigate(route: string) {
    setOpen(false);
    router.push(route as never);
  }

  function closeDrawer() {
    Animated.parallel([
      Animated.timing(backdropOpacity, { toValue: 0, duration: animation.fast, useNativeDriver: true }),
      Animated.timing(drawerSlide, { toValue: -320, duration: animation.fast, useNativeDriver: true }),
    ]).start(() => setOpen(false));
  }

  return (
    <MatteBackground style={{ paddingTop: insets.top }}>
      <MatteSurface variant="elevated" borderRadius={0} padded={false} style={styles.headerWrap}>
        <View style={styles.header}>
          <AnimatedPressable style={styles.menuButton} onPress={() => setOpen(true)} accessibilityLabel="Open menu">
            <Icon name="menu" size="md" color={colors.text} />
          </AnimatedPressable>
          <View style={styles.headerText}>
            <Text style={styles.headerTitle}>{title}</Text>
            {activeFamily ? (
              <Text style={styles.headerSubtitle}>{activeFamily.displayName}</Text>
            ) : session ? (
              <Text style={styles.headerSubtitle}>{session.displayName}</Text>
            ) : null}
          </View>
          <View
            style={[
              styles.headerBadge,
              (hasAdvancedAccess(session) || isPlatformAdmin(session)) && styles.headerBadgeAdvanced,
            ]}
          >
            <Text style={styles.headerBadgeText}>
              {isPlatformAdmin(session) ? "ADM" : hasAdvancedAccess(session) ? "ADV" : "PHI"}
            </Text>
          </View>
        </View>
      </MatteSurface>

      <View style={styles.content}>{children}</View>

      <Modal visible={open} transparent animationType="none" onRequestClose={closeDrawer}>
        <View style={styles.modalRoot}>
          <Pressable style={StyleSheet.absoluteFill} onPress={closeDrawer}>
            <Animated.View style={[styles.backdrop, { opacity: backdropOpacity }]} />
          </Pressable>
          <Animated.View
            style={[
              styles.drawerOuter,
              {
                paddingTop: insets.top + spacing.md,
                paddingBottom: insets.bottom + spacing.md,
                transform: [{ translateX: drawerSlide }],
              },
            ]}
          >
            <MatteSurface variant="elevated" borderRadius={0} padded={false} elevated={false} style={styles.drawerGlass}>
              <View style={styles.drawer}>
                <View style={styles.drawerBrandRow}>
                  <Icon name="pulse" size="md" color={colors.primary} />
                  <Text style={styles.drawerBrand}>Personal Health Intelligence</Text>
                </View>

                {session && (
                  <View style={styles.drawerUser}>
                    <View style={styles.drawerAvatar}>
                      <Text style={styles.drawerAvatarText}>{session.displayName.charAt(0)}</Text>
                    </View>
                    <View style={styles.drawerUserInfo}>
                      <Text style={styles.drawerUserName}>{session.displayName}</Text>
                      <Text style={styles.drawerUserEmail}>{session.email}</Text>
                      {activeFamily && <Text style={styles.drawerFamily}>{activeFamily.displayName}</Text>}
                    </View>
                  </View>
                )}

                <View style={styles.menuList}>
                  {menuItems.map((item) => {
                    const active = isActiveRoute(pathname, item.route);
                    const icons = navIcons[item.iconKey];
                    const iconName: IconName = active ? icons.active : icons.inactive;
                    return (
                      <AnimatedPressable
                        key={item.route}
                        style={[styles.menuItem, active && styles.menuItemActive]}
                        onPress={() => navigate(item.route)}
                      >
                        <Icon
                          name={iconName}
                          size="md"
                          color={active ? colors.primaryDark : colors.textMuted}
                        />
                        <Text style={[styles.menuItemLabel, active && styles.menuItemLabelActive]}>
                          {item.label}
                        </Text>
                      </AnimatedPressable>
                    );
                  })}
                </View>

                <View style={styles.drawerFooter}>
                  <AnimatedPressable style={styles.signOutButton} onPress={handleSignOut}>
                    <Icon name="log-out-outline" size="md" color={colors.danger} />
                    <Text style={styles.signOutText}>Sign out</Text>
                  </AnimatedPressable>
                </View>
              </View>
            </MatteSurface>
          </Animated.View>
        </View>
      </Modal>
    </MatteBackground>
  );
}
