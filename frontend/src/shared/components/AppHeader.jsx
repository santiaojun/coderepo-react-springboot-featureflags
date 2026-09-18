import { BrandMark } from "./BrandMark.jsx";
import { MaterialIcon } from "./MaterialIcon.jsx";

export function AppHeader({ children, onSidebarToggle, trailing }) {
    return <header className="app-header">
        <button aria-label="Toggle navigation" className="icon-button header-menu" type="button" onClick={onSidebarToggle}>
            <MaterialIcon size={22}>menu</MaterialIcon>
        </button>
        <div className="brand">
            <BrandMark />
            <span>Flagpost</span>
        </div>
        {children}
        <div className="header-actions">{trailing}</div>
    </header>;
}
