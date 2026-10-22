import markUrl from "./assets/schimmelpilz-mark.webp";

const claim = {
  de: "Entwickelt von einem ausgebildeten Rechtsanwaltsfachangestellten",
  en: "Developed by a trained German paralegal",
};

// CI lockup: badge plus live wordmark in Plus Jakarta Sans, so the name stays sharp at every size.
// Size follows --brand-size (font size of the lockup); `compact` drops the claim line.
export function BrandLogo({ className = "brand-logo", lang = "de", compact = false }) {
  return <span className={`brand-lockup ${compact ? "compact" : ""} ${className}`} role="img" aria-label={`Rechtsassistentin Schimmelpilz${compact ? "" : ` – ${claim[lang] || claim.de}`}`}>
    <img className="brand-mark" src={markUrl} alt="" width="141" height="142" />
    <span className="brand-words" aria-hidden="true">
      <span className="brand-kicker">Rechtsassistentin</span>
      <span className="brand-name">Schimmelpilz</span>
      {!compact && <span className="brand-claim">{claim[lang] || claim.de}</span>}
    </span>
  </span>;
}
