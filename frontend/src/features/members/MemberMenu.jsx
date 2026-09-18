import { useEffect, useRef, useState } from "react";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { MemberAvatar } from "./MemberAvatar.jsx";

export function MemberMenu({ member, onLogout, onSwitchMember, onThemeChange, theme }) {
    const [open, setOpen] = useState(false);
    const anchor = useRef(null);
    useEffect(() => {
        if (!open) return undefined;
        const closeOutside = (event) => { if (!anchor.current?.contains(event.target)) setOpen(false); };
        const closeOnEscape = (event) => { if (event.key === "Escape") setOpen(false); };
        document.addEventListener("pointerdown", closeOutside);
        document.addEventListener("keydown", closeOnEscape);
        return () => { document.removeEventListener("pointerdown", closeOutside); document.removeEventListener("keydown", closeOnEscape); };
    }, [open]);
    return <div className="profile-menu-anchor" ref={anchor}>
        <button aria-expanded={open} aria-haspopup="menu" aria-label={`Flagpost member: ${member.name}`} className="header-profile-button" type="button" onClick={() => setOpen((value) => !value)}>
            <MemberAvatar member={member} size="small" />
        </button>
        {open && <div className="profile-menu" role="menu">
            <div className="profile-menu-identity">
                <p className="profile-menu-label">SIGNED IN AS</p>
                <MemberAvatar member={member} />
                <strong>{member.name}</strong>
                <span>{member.email}</span>
                <span>{member.title} · {member.role}</span>
            </div>
            <div className="profile-menu-appearance">
                <span>Appearance</span>
                <div className="theme-toggle" role="group" aria-label="Color theme">
                    <button aria-pressed={theme === "dark"} type="button" onClick={() => onThemeChange("dark")}>
                        <MaterialIcon size={17}>dark_mode</MaterialIcon>Dark
                    </button>
                    <button aria-pressed={theme === "light"} type="button" onClick={() => onThemeChange("light")}>
                        <MaterialIcon size={17}>light_mode</MaterialIcon>Light
                    </button>
                </div>
            </div>
            <button className="profile-switch-button" role="menuitem" type="button" onClick={() => { setOpen(false); onSwitchMember(); }}>
                <span className="profile-switch-icon"><MaterialIcon size={19}>group</MaterialIcon></span>
                <span><strong>Switch member</strong><small>Act as another teammate</small></span>
                <MaterialIcon size={18}>chevron_right</MaterialIcon>
            </button>
            <button className="profile-signout-button" role="menuitem" type="button" onClick={() => { setOpen(false); onLogout(); }}>
                <MaterialIcon size={18}>logout</MaterialIcon>Sign out
            </button>
        </div>}
    </div>;
}
