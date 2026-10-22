import logoUrl from "./assets/schimmelpilz-logo.webp?inline";

// Bundle the supplied logo with the app so it needs no separate asset request.
// 900 px wide WebP (from the 2172 px original PNG) is sharp at its largest ~300 px display width and keeps the bundle small.
export function BrandLogo({ className = "brand-logo" }) {
  return <img className={className} src={logoUrl} alt="Virtuelle Rechtsassistenz Schimmelpilz" width="900" height="300" />;
}
