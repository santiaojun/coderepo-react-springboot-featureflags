export function BrandMark({ size = "medium" }) {
    return <span aria-hidden="true" className={`brand-mark brand-mark-${size}`}><span /></span>;
}
