import { useCallback, useEffect, useMemo, useState } from "react";
import { AppHeader } from "./shared/components/AppHeader.jsx";
import { AppSidebar } from "./shared/components/AppSidebar.jsx";
import { BrandMark } from "./shared/components/BrandMark.jsx";
import { hasProfileToken, hasSessionToken, setProfileToken, setSessionToken } from "./shared/api/client.js";
import { applyTheme, readTheme } from "./shared/utils/theme.js";
import { authApi } from "./features/auth/auth.api.js";
import { WorkspaceLogin } from "./features/auth/WorkspaceLogin.jsx";
import { memberApi } from "./features/members/member.api.js";
import { MemberMenu } from "./features/members/MemberMenu.jsx";
import { MemberPicker } from "./features/members/MemberPicker.jsx";
import { environmentApi } from "./features/environments/environment.api.js";
import { EnvironmentSummary } from "./features/environments/EnvironmentSummary.jsx";
import { EnvironmentSwitcher } from "./features/environments/EnvironmentSwitcher.jsx";
import { flagApi } from "./features/flags/flag.api.js";
import { FlagDetail } from "./features/flags/FlagDetail.jsx";
import { FlagEditor } from "./features/flags/FlagEditor.jsx";
import { FlagList } from "./features/flags/FlagList.jsx";
import { segmentApi } from "./features/segments/segment.api.js";
import { SegmentList } from "./features/segments/SegmentList.jsx";
import { AudiencePage } from "./features/audience/AudiencePage.jsx";
import { HistoryPage } from "./features/history/HistoryPage.jsx";
import { ConfirmationDialog } from "./shared/components/ConfirmationDialog.jsx";
import { servingLabel } from "./features/flags/flag-format.js";

const DEFAULT_FILTERS = { search: "", tag: "", state: "all", status: "active" };

function AppBootScreen() {
    return <main className="app-boot" aria-label="Opening Flagpost" role="status">
        <div className="app-boot-brand"><BrandMark size="large" /><strong>Flagpost</strong></div>
        <div className="app-boot-progress" aria-hidden="true"><span /></div>
    </main>;
}

export function FlagpostWorkspace({ activeMember, members, onLogout, onSwitchMember }) {
    const [theme, setTheme] = useState(readTheme);
    const [view, setViewState] = useState(() => {
        const stored = localStorage.getItem("flagpost-view");
        return ["flags", "segments", "audience", "history"].includes(stored) ? stored : "flags";
    });
    const [sidebarCollapsed, setSidebarCollapsed] = useState(() =>
        window.matchMedia?.("(max-width: 900px)").matches ?? false);
    const [environments, setEnvironments] = useState([]);
    const [environmentKey, setEnvironmentKey] = useState(() => localStorage.getItem("flagpost-environment") || "");
    const [bootError, setBootError] = useState("");
    const [booting, setBooting] = useState(true);
    const [summary, setSummary] = useState(null);
    const [summaryStatus, setSummaryStatus] = useState({ loading: true, error: "" });
    const [flags, setFlags] = useState([]);
    const [tags, setTags] = useState([]);
    const [flagStatus, setFlagStatus] = useState({ loading: true, error: "" });
    const [filters, setFilters] = useState(DEFAULT_FILTERS);
    const [segments, setSegments] = useState([]);
    const [selectedFlagKey, setSelectedFlagKey] = useState(null);
    const [creating, setCreating] = useState(false);
    const [togglePending, setTogglePending] = useState("");
    const [toggleError, setToggleError] = useState("");
    const [confirmToggle, setConfirmToggle] = useState(null);

    const environment = useMemo(
        () => environments.find((entry) => entry.key === environmentKey) || environments[0] || null,
        [environments, environmentKey],
    );

    const setView = (next) => {
        setViewState(next);
        setSelectedFlagKey(null);
        localStorage.setItem("flagpost-view", next);
        if (window.matchMedia?.("(max-width: 900px)").matches) setSidebarCollapsed(true);
    };

    const changeEnvironment = (key) => {
        setEnvironmentKey(key);
        localStorage.setItem("flagpost-environment", key);
    };

    const changeTheme = (next) => setTheme(applyTheme(next));

    useEffect(() => { applyTheme(theme); }, [theme]);

    // Below 900px the sidebar becomes an overlay drawer. Follow the breakpoint so a
    // window resize or an orientation change never leaves the drawer covering the page.
    useEffect(() => {
        const query = window.matchMedia?.("(max-width: 900px)");
        if (!query) return undefined;
        const sync = (event) => setSidebarCollapsed(event.matches);
        query.addEventListener("change", sync);
        return () => query.removeEventListener("change", sync);
    }, []);

    useEffect(() => {
        let active = true;
        (async () => {
            try {
                const list = await environmentApi.list();
                if (!active) return;
                setEnvironments(list);
                if (!list.some((entry) => entry.key === environmentKey)) {
                    const fallback = list.find((entry) => entry.key === "development") || list[0];
                    if (fallback) changeEnvironment(fallback.key);
                }
                setBootError("");
            } catch (error) {
                if (active) setBootError(error.message);
            } finally {
                if (active) setBooting(false);
            }
        })();
        return () => { active = false; };
    }, []);

    const loadSummary = useCallback(async () => {
        if (!environment) return;
        setSummaryStatus({ loading: true, error: "" });
        try {
            setSummary(await environmentApi.summary(environment.key));
            setSummaryStatus({ loading: false, error: "" });
        } catch (error) {
            setSummaryStatus({ loading: false, error: error.message });
        }
    }, [environment]);

    const loadFlags = useCallback(async () => {
        if (!environment) return;
        setFlagStatus({ loading: true, error: "" });
        try {
            const data = await flagApi.list(environment.key, {
                search: filters.search.trim(),
                tag: filters.tag,
                state: filters.state,
                status: filters.status,
            });
            setFlags(data.flags);
            setTags(data.tags);
            setFlagStatus({ loading: false, error: "" });
        } catch (error) {
            setFlagStatus({ loading: false, error: error.message });
            setFlags([]);
        }
    }, [environment, filters]);

    const loadSegments = useCallback(async () => {
        if (!environment) return;
        try {
            const data = await segmentApi.list(environment.key);
            setSegments(data.segments);
        } catch {
            setSegments([]);
        }
    }, [environment]);

    useEffect(() => { loadSummary(); }, [loadSummary]);
    useEffect(() => { loadSegments(); }, [loadSegments]);
    useEffect(() => {
        if (!environment || view !== "flags" || selectedFlagKey) return undefined;
        const timer = setTimeout(loadFlags, filters.search ? 220 : 0);
        return () => clearTimeout(timer);
    }, [loadFlags, environment, view, selectedFlagKey, filters.search]);

    const applyToggle = async (flag, next) => {
        setTogglePending(flag.key);
        setToggleError("");
        try {
            await flagApi.toggle(flag.key, environment.key, { enabled: next, version: flag.configuration.version });
            setConfirmToggle(null);
            await Promise.all([loadFlags(), loadSummary()]);
        } catch (error) {
            setToggleError(error.message);
        } finally {
            setTogglePending("");
        }
    };

    const requestToggle = (flag, next) => {
        if (environment?.critical) setConfirmToggle({ flag, next });
        else applyToggle(flag, next);
    };

    if (booting) return <AppBootScreen />;

    if (bootError || !environment) {
        return <main className="workspace-login-page">
            <section className="workspace-login-card">
                <div className="workspace-login-mark"><BrandMark size="large" /></div>
                <h1>Flagpost is unavailable</h1>
                <p className="workspace-login-subtitle">{bootError || "No environments are configured."}</p>
                <div className="workspace-login-actions">
                    <button className="primary-button" type="button" onClick={() => window.location.reload()}>Reload</button>
                </div>
            </section>
        </main>;
    }

    return <div className={`app-shell ${sidebarCollapsed ? "sidebar-collapsed" : ""}`}>
        <AppHeader
            onSidebarToggle={() => setSidebarCollapsed((value) => !value)}
            trailing={<MemberMenu
                member={activeMember}
                theme={theme}
                onLogout={onLogout}
                onSwitchMember={onSwitchMember}
                onThemeChange={changeTheme}
            />}
        >
            <EnvironmentSwitcher environments={environments} value={environment.key} onChange={changeEnvironment} />
        </AppHeader>

        <AppSidebar collapsed={sidebarCollapsed} view={view} onSelect={setView}>
            <EnvironmentSummary
                error={summaryStatus.error}
                loading={summaryStatus.loading}
                summary={summary}
                onRetry={loadSummary}
            />
        </AppSidebar>

        {!sidebarCollapsed && <button className="sidebar-scrim" aria-label="Close navigation" onClick={() => setSidebarCollapsed(true)} />}

        <main className={`workspace-surface environment-${environment.key}`}>
            {toggleError && !confirmToggle && <div className="service-error inline" role="alert">
                <span>{toggleError}</span>
                <button type="button" onClick={() => { setToggleError(""); loadFlags(); }}>Reload</button>
            </div>}

            {view === "flags" && (selectedFlagKey
                ? <FlagDetail
                    environment={environment}
                    flagKey={selectedFlagKey}
                    members={members}
                    segments={segments}
                    onBack={() => { setSelectedFlagKey(null); loadFlags(); loadSummary(); }}
                    onChanged={() => { loadSummary(); loadSegments(); }}
                />
                : <FlagList
                    environment={environment}
                    error={flagStatus.error}
                    filters={filters}
                    flags={flags}
                    loading={flagStatus.loading}
                    pendingKey={togglePending}
                    tags={tags}
                    onCreate={() => setCreating(true)}
                    onFiltersChange={setFilters}
                    onOpen={setSelectedFlagKey}
                    onRetry={loadFlags}
                    onToggle={requestToggle}
                />)}

            {view === "segments" && <SegmentList environment={environment} onSegmentsChanged={loadSegments} />}
            {view === "audience" && <AudiencePage environment={environment} />}
            {view === "history" && <HistoryPage environment={environment} members={members} />}
        </main>

        {creating && <FlagEditor
            onClose={() => setCreating(false)}
            onSave={async (payload) => {
                const created = await flagApi.create(payload);
                setCreating(false);
                await Promise.all([loadFlags(), loadSummary()]);
                setSelectedFlagKey(created.flag.key);
            }}
        />}

        {confirmToggle && <ConfirmationDialog
            busy={Boolean(togglePending)}
            confirmLabel={confirmToggle.next ? `Turn on in ${environment.name}` : `Turn off in ${environment.name}`}
            error={toggleError}
            message={confirmToggle.next
                ? `${confirmToggle.flag.key} will start serving ${servingLabel(confirmToggle.flag, confirmToggle.flag.configuration)} to live traffic.`
                : `${confirmToggle.flag.key} will serve its off variation to everybody in ${environment.name}.`}
            title={`${confirmToggle.next ? "Turn on" : "Turn off"} in ${environment.name}?`}
            onCancel={() => { setConfirmToggle(null); setToggleError(""); }}
            onConfirm={() => applyToggle(confirmToggle.flag, confirmToggle.next)}
        />}
    </div>;
}

export default function App() {
    const [account, setAccount] = useState(null);
    const [authLoading, setAuthLoading] = useState(false);
    const [bootstrapping, setBootstrapping] = useState(true);
    const [authError, setAuthError] = useState("");
    const [members, setMembers] = useState(null);
    const [memberError, setMemberError] = useState("");
    const [selectedMemberId, setSelectedMemberId] = useState(() => localStorage.getItem("flagpost-profile-id") || "");
    const [switchingFrom, setSwitchingFrom] = useState("");

    const loadMembers = useCallback(async () => {
        try { setMemberError(""); setMembers(await memberApi.list()); }
        catch (error) { setMemberError(error.message); setMembers([]); }
    }, []);

    useEffect(() => {
        const expire = (event) => {
            setProfileToken(""); setSessionToken(""); localStorage.removeItem("flagpost-profile-id");
            setSelectedMemberId(""); setAccount(null); setMembers(null);
            setAuthError(event.detail || "Your Flagpost session has expired.");
        };
        window.addEventListener("flagpost-session-expired", expire);
        return () => window.removeEventListener("flagpost-session-expired", expire);
    }, []);

    useEffect(() => {
        let active = true;
        const restore = async () => {
            if (!hasSessionToken()) { if (active) setBootstrapping(false); return; }
            let restoredSession = null;
            let restoreError = null;
            try {
                restoredSession = await authApi.session();
            } catch (error) {
                restoreError = error;
                if (hasProfileToken()) {
                    setProfileToken("");
                    localStorage.removeItem("flagpost-profile-id");
                    try { restoredSession = await authApi.session(); restoreError = null; }
                    catch (sessionError) { restoreError = sessionError; }
                }
            }
            if (!active) return;
            if (!restoredSession) {
                setProfileToken(""); setSessionToken(""); localStorage.removeItem("flagpost-profile-id");
                setAuthError(restoreError?.message || "Your Flagpost session has expired.");
                setBootstrapping(false);
                return;
            }
            let restoredMembers = [];
            let restoredMemberError = "";
            try { restoredMembers = await memberApi.list(); }
            catch (memberRequestError) { restoredMemberError = memberRequestError.message; }
            if (!active) return;
            setAccount(restoredSession.account);
            setMembers(restoredMembers);
            setAuthError("");
            setMemberError(restoredMemberError);
            setBootstrapping(false);
        };
        restore();
        return () => { active = false; };
    }, []);

    const activeMember = members?.find((member) => String(member._id) === selectedMemberId);

    useEffect(() => {
        if (members === null) return;
        if (selectedMemberId && !activeMember) {
            localStorage.removeItem("flagpost-profile-id");
            setProfileToken("");
            setSelectedMemberId("");
        }
    }, [activeMember, members, selectedMemberId]);

    const selectMember = async (member) => {
        try {
            setMemberError("");
            const result = await authApi.switchProfile(member._id);
            setProfileToken(result.token);
            localStorage.setItem("flagpost-profile-id", member._id);
            setSelectedMemberId(String(member._id));
            setSwitchingFrom("");
        } catch (error) { setMemberError(error.message); }
    };

    const login = async (email, password) => {
        try {
            setAuthLoading(true); setAuthError("");
            const result = await authApi.login(email, password);
            setSessionToken(result.token);
            let discovered = [];
            try { setMemberError(""); discovered = await memberApi.list(); }
            catch (memberRequestError) { setMemberError(memberRequestError.message); }
            setMembers(discovered);
            setAccount(result.account);
            const own = discovered.find((member) => member.email === result.account.email);
            if (own) await selectMember(own);
        } catch (error) { setAuthError(error.message); }
        finally { setAuthLoading(false); }
    };

    const logout = async () => {
        try { if (hasSessionToken()) await authApi.logout(); } catch { /* the token is discarded regardless */ }
        setProfileToken(""); setSessionToken(""); localStorage.removeItem("flagpost-profile-id");
        setSelectedMemberId(""); setSwitchingFrom(""); setAccount(null); setMembers(null); setAuthError("");
    };

    if (bootstrapping) return <AppBootScreen />;
    if (!account) return <WorkspaceLogin error={authError} loading={authLoading} onLogin={login} />;
    if (activeMember) {
        return <FlagpostWorkspace
            activeMember={activeMember}
            key={activeMember._id}
            members={members || []}
            onLogout={logout}
            onSwitchMember={() => {
                setSwitchingFrom(selectedMemberId);
                localStorage.removeItem("flagpost-profile-id");
                setProfileToken("");
                setSelectedMemberId("");
            }}
        />;
    }
    return <MemberPicker
        error={memberError}
        loading={members === null}
        members={(members || []).filter((member) => String(member._id) !== switchingFrom)}
        switchingFrom={members?.find((member) => String(member._id) === switchingFrom)}
        onLogout={logout}
        onRetry={loadMembers}
        onSelect={selectMember}
    />;
}
