import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from "react";
import { FamilySwitchOverlay } from "../components/FamilySwitchOverlay";
import {
  FamilyDetail,
  FamilyMember,
  FamilySummary,
  getFamily,
  listFamilies,
} from "./api";
import { clearViewedPerson, getViewedPerson, setViewedPerson as persistViewedPerson } from "./activePerson";
import { getActiveFamilyId, setActiveFamilyId } from "./activeFamily";
import { getSession } from "./auth";
import { hasAdvancedAccess } from "./session";

type FamilyContextValue = {
  families: FamilySummary[];
  activeFamily: FamilySummary | null;
  familyDetail: FamilyDetail | null;
  members: FamilyMember[];
  viewedPersonId: number | null;
  viewedPersonName: string | null;
  isLoading: boolean;
  isSwitching: boolean;
  switchFamily: (familyId: string) => Promise<void>;
  refreshFamilies: () => Promise<void>;
  setViewedPerson: (personId: number, displayName: string) => Promise<void>;
  clearViewedPerson: () => Promise<void>;
  effectivePersonId: (selfPersonId: number) => number;
  isViewingOther: (selfPersonId: number) => boolean;
  viewedMember: FamilyMember | null;
};

const FamilyContext = createContext<FamilyContextValue | null>(null);

export function FamilyProvider({ children }: { children: ReactNode }) {
  const [families, setFamilies] = useState<FamilySummary[]>([]);
  const [activeFamily, setActiveFamily] = useState<FamilySummary | null>(null);
  const [familyDetail, setFamilyDetail] = useState<FamilyDetail | null>(null);
  const [members, setMembers] = useState<FamilyMember[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSwitching, setIsSwitching] = useState(false);
  const [overlayName, setOverlayName] = useState("");
  const [showOverlay, setShowOverlay] = useState(false);
  const [viewedPersonId, setViewedPersonId] = useState<number | null>(null);
  const [viewedPersonName, setViewedPersonName] = useState<string | null>(null);

  const syncViewedPersonWithMembers = useCallback((nextMembers: FamilyMember[]) => {
    if (viewedPersonId == null) {
      return;
    }
    const stillPresent = nextMembers.some((member) => member.personId === viewedPersonId);
    if (!stillPresent) {
      setViewedPersonId(null);
      setViewedPersonName(null);
      clearViewedPerson().catch(() => undefined);
    }
  }, [viewedPersonId]);

  const loadFamilyData = useCallback(async (familyId: string) => {
    const detail = await getFamily(familyId);
    setFamilyDetail(detail);
    setMembers(detail.members);
    syncViewedPersonWithMembers(detail.members);
    return detail;
  }, [syncViewedPersonWithMembers]);

  const refreshFamilies = useCallback(async () => {
    setIsLoading(true);
    try {
      const session = await getSession();
      if (!session?.token) {
        setFamilies([]);
        setActiveFamily(null);
        setFamilyDetail(null);
        setMembers([]);
        return;
      }

      const list = await listFamilies();
      setFamilies(list);

      if (list.length === 0) {
        setActiveFamily(null);
        setFamilyDetail(null);
        setMembers([]);
        return;
      }

      const storedId = await getActiveFamilyId();
      const target = list.find((f) => f.id === storedId) ?? list[0];
      setActiveFamily(target);
      await setActiveFamilyId(target.id);

      if (hasAdvancedAccess(session)) {
        try {
          await loadFamilyData(target.id);
        } catch {
          setFamilyDetail(null);
          setMembers([]);
        }
      } else {
        setFamilyDetail(null);
        setMembers([]);
      }
    } catch {
      setFamilies([]);
      setActiveFamily(null);
      setFamilyDetail(null);
      setMembers([]);
    } finally {
      setIsLoading(false);
    }
  }, [loadFamilyData]);

  useEffect(() => {
    refreshFamilies();
  }, [refreshFamilies]);

  useEffect(() => {
    getViewedPerson()
      .then((viewed) => {
        if (viewed) {
          setViewedPersonId(viewed.personId);
          setViewedPersonName(viewed.displayName);
        }
      })
      .catch(() => undefined);
  }, []);

  const handleSetViewedPerson = useCallback(async (personId: number, displayName: string) => {
    await persistViewedPerson(personId, displayName);
    setViewedPersonId(personId);
    setViewedPersonName(displayName);
  }, []);

  const handleClearViewedPerson = useCallback(async () => {
    await clearViewedPerson();
    setViewedPersonId(null);
    setViewedPersonName(null);
  }, []);

  const effectivePersonId = useCallback(
    (selfPersonId: number) => viewedPersonId ?? selfPersonId,
    [viewedPersonId]
  );

  const isViewingOther = useCallback(
    (selfPersonId: number) => viewedPersonId != null && viewedPersonId !== selfPersonId,
    [viewedPersonId]
  );

  const viewedMember =
    viewedPersonId != null ? members.find((member) => member.personId === viewedPersonId) ?? null : null;

  const switchFamily = useCallback(
    async (familyId: string) => {
      if (activeFamily?.id === familyId) return;

      const next = families.find((f) => f.id === familyId);
      if (!next) return;

      setIsSwitching(true);
      setOverlayName(next.displayName);
      setShowOverlay(true);

      await setActiveFamilyId(familyId);
      setActiveFamily(next);

      const session = await getSession();
      if (hasAdvancedAccess(session)) {
        try {
          await loadFamilyData(familyId);
        } catch {
          setFamilyDetail(null);
          setMembers([]);
        }
      } else {
        setFamilyDetail(null);
        setMembers([]);
      }
    },
    [activeFamily, families, loadFamilyData]
  );

  function handleOverlayFinished() {
    setShowOverlay(false);
    setIsSwitching(false);
  }

  return (
    <FamilyContext.Provider
      value={{
        families,
        activeFamily,
        familyDetail,
        members,
        viewedPersonId,
        viewedPersonName,
        isLoading,
        isSwitching,
        switchFamily,
        refreshFamilies,
        setViewedPerson: handleSetViewedPerson,
        clearViewedPerson: handleClearViewedPerson,
        effectivePersonId,
        isViewingOther,
        viewedMember,
      }}
    >
      {children}
      <FamilySwitchOverlay
        visible={showOverlay}
        familyName={overlayName}
        onFinished={handleOverlayFinished}
      />
    </FamilyContext.Provider>
  );
}

export function useFamily() {
  const ctx = useContext(FamilyContext);
  if (!ctx) {
    throw new Error("useFamily must be used within FamilyProvider");
  }
  return ctx;
}
