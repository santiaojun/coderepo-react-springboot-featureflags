import { identityInitials } from "../../shared/utils/identity.js";

export function MemberAvatar({ member, size = "medium" }) {
    return <span className={`profile-avatar profile-avatar-${size}`} style={{ "--profile-color": member.avatarColor }} aria-hidden="true">
        <span>{identityInitials(member.name)}</span>
    </span>;
}
