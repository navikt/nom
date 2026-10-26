// TODO: Fjerne unødvendige Consts i filer som bruker disse, heller bruke dem direkte

export function isInDevelopment() {
  const match = globalThis.location.hostname.match(/localhost|\.dev\./);

  return !!match;
}

export function navToppnodeOrgEnhetId() {
  return isInDevelopment() ? "sa312u" : "py437s";
}

export function direktoratOrgEnhetId() {
  return isInDevelopment() ? "fa965e" : "bu431e";
}

export function teknologiavdelingenOrgEnhetId() {
  return isInDevelopment() ? "je104d" : "te627k";
}

export function helfoOrgEnhetId() {
  return isInDevelopment() ? "mu405g" : "je305h";
}

export function skatteetatenOrgEnhetId() {
  return isInDevelopment() ? "ja464g" : "ne938h";
}

export function riksrevisjonenOrgEnhetId() {
  return isInDevelopment() ? "ge146b" : "ke602d";
}

export function vikafossenOrgEnhetId() {
  return isInDevelopment() ? "ru151s" : "ru664y";
}

export function forsideOrgEnhetIder() {
  const developmentForsideOrgEnhetIder = [
    "fa965e",
    "me177r",
    "ju219d",
    "hu797w",
    "tu734a",
  ];
  const productionForsideOrgEnhetIder = [
    "bu431e",
    "ry630r",
    "te464t",
    "se232x",
    "ne994m",
  ];

  return isInDevelopment()
    ? developmentForsideOrgEnhetIder
    : productionForsideOrgEnhetIder;
}

export function isHelfoOrgEnhet(orgEnhetId: string) {
  return orgEnhetId === helfoOrgEnhetId();
}

export function isSkatteetatenOrgEnhet(orgEnhetId: string) {
  return orgEnhetId === skatteetatenOrgEnhetId();
}

export function isRiksrevisjonenOrgEnhet(orgEnhetId: string) {
  return orgEnhetId === riksrevisjonenOrgEnhetId();
}

export function isVikafossenOrgEnhet(orgEnhetId: string) {
  return orgEnhetId === vikafossenOrgEnhetId();
}

export function isEksternSamarbeidspartnerOrgEnhet(orgEnhetId: string) {
  return (
    isHelfoOrgEnhet(orgEnhetId) ||
    isSkatteetatenOrgEnhet(orgEnhetId) ||
    isRiksrevisjonenOrgEnhet(orgEnhetId) ||
    isVikafossenOrgEnhet(orgEnhetId)
  );
}

export function eksterneOrgEnheter() {
  return isInDevelopment()
    ? ["fa520s", "fe857w", "hy255y", "ja464g", "py733e", "ru151s"]
    : ["fe178t", "hu192a", "ne938h", "va111j", "va981f", "ru664y", "ke602d"];
}

export function teamkatalogenBaseUrl() {
  return isInDevelopment()
    ? "https://teamkatalog.ekstern.dev.nav.no"
    : "https://teamkatalog.nav.no";
}

export function adminBaseUrl() {
  return isInDevelopment()
    ? "https://nom-admin.ansatt.dev.nav.no"
    : "https://nom-admin.ansatt.nav.no";
}
