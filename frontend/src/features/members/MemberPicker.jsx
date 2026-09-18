import { BrandMark } from "../../shared/components/BrandMark.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { MemberAvatar } from "./MemberAvatar.jsx";

export function MemberPicker({ error = "", loading = false, members = [], onLogout, onRetry, onSelect, switchingFrom }) {
    return <main className="profile-picker-page">
        <section className="profile-picker-card" aria-labelledby="member-picker-title">
            <div className="profile-picker-mark"><BrandMark size="large" /></div>
            <h1 id="member-picker-title">Choose a member</h1>
            <p className="profile-picker-subtitle">to continue to Flagpost</p>
            {switchingFrom && <p className="profile-picker-current">Signed in as {switchingFrom.name}</p>}
            {loading && <div className="profile-picker-status" role="status">Loading members…</div>}
            {!loading && error && <div className="profile-picker-error" role="alert">
                <div><strong>Members are unavailable</strong><span>{error}</span></div>
                <button type="button" onClick={onRetry}>Retry</button>
            </div>}
            {!loading && !error && members.length === 0 && <div className="profile-picker-status" role="status">
                No members are available in this workspace.
            </div>}
            {!loading && !error && members.length > 0 && <ul className="profile-picker-list">
                {members.map((member) => <li key={member._id}>
                    <button className="profile-switch-button" type="button" onClick={() => onSelect(member)}>
                        <MemberAvatar member={member} size="small" />
                        <span><strong>{member.name}</strong><small>{member.title} · {member.role}</small></span>
                        <MaterialIcon size={18}>chevron_right</MaterialIcon>
                    </button>
                </li>)}
            </ul>}
            <div className="profile-picker-footer">
                <button type="button" onClick={onLogout}>Use another account</button>
            </div>
        </section>
    </main>;
}
