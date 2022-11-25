package no.nav.nom.contract.nom.state;

import java.util.Optional;

public record OrgEnhetState (
    String fid,
    String id,  // Ikke kryptisk uid, ment for menneskelig daglig bruk
    String navn,
    Optional<String> overordnetOrgEnhetFid,
    Optional<OrgEnhetState> overordnetOrgEnhet,
    Optional<String> lederFid,
    Optional<RessursState> leder
) {}
