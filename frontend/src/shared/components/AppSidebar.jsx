import { MaterialIcon } from "./MaterialIcon.jsx";

const NAV_ITEMS = [
    { value: "flags", label: "Feature flags", icon: "subject" },
    { value: "segments", label: "Segments", icon: "group" },
    { value: "audience", label: "Audience", icon: "person" },
    { value: "history", label: "Change history", icon: "history" },
];

export function AppSidebar({ children, collapsed, onSelect, view }) {
    return <nav aria-label="Flagpost sections" className={`sidebar ${collapsed ? "collapsed" : ""}`}>
        <div className="sidebar-inner">
            <ul className="sidebar-nav">
                {NAV_ITEMS.map((item) => <li key={item.value}>
                    <button
                        aria-current={view === item.value ? "page" : undefined}
                        className={view === item.value ? "active" : ""}
                        type="button"
                        onClick={() => onSelect(item.value)}
                    >
                        <MaterialIcon size={19}>{item.icon}</MaterialIcon>
                        <span>{item.label}</span>
                    </button>
                </li>)}
            </ul>
            {children}
        </div>
    </nav>;
}
