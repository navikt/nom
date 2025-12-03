package no.nav.nom.apps.gql.hierarchygeneration.builder;


import no.nav.nom.apps.gql.hierarchygeneration.NavIdent;
import no.nav.nom.apps.gql.hierarchygeneration.NomId;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class InitHierarkiIdGenerator {
    private final AtomicInteger count = new AtomicInteger(0);

    public int getNextIdNumber() {
        var lastBeforeOverflow = 20 * 1000;
        var nextNumber = count.incrementAndGet();
        if (nextNumber > lastBeforeOverflow) {
            throw new IllegalStateException("Id generation overflowed. Reached " + lastBeforeOverflow);
        }
        return nextNumber;
    }

    public NomId generateNomId() {

        var nextNum = this.getNextIdNumber();
        var maxThreshold = (40 * 1000) - 1;
        if (nextNum > maxThreshold) {
            throw new IllegalStateException("ID generation overflowed");
        }

        var numberPartStr = nextNum % 1000 + "";
        var prefixPartNumber = nextNum / 1000;

        var prefixLetter1 = "" + "ptrmfdnjhs".charAt(prefixPartNumber % 10);
        var prefixLetter2 = "" + "aeuy".charAt(prefixPartNumber / 10);

        var numbersMiddle = "0".repeat(3 - numberPartStr.length()) + numberPartStr;
        var outStr = prefixLetter1 + prefixLetter2 + numbersMiddle + "b";
        return NomId.create(outStr);
    }

    public NavIdent generateNavIdent() {
        var suffixTmp = this.getNextIdNumber() + "";
        var suffix = "0".repeat(6 - suffixTmp.length()) + suffixTmp;
        var outStr = "X" + suffix;
        return NavIdent.create(outStr);
    }

    public OrgEnhetState.OrgEnhetStateBuilder orgEnhetStateBuilder(String navn) {
        var nomIdStr = generateNomId().nomId();
        return OrgEnhetState.builder()
            .id(nomIdStr)
            .fid("fid-" + nomIdStr)
            .agressoId("agresso-" + nomIdStr)
            .navn(navn == null ? "Navn " + nomIdStr : navn);
    }

    public RessursState.RessursStateBuilder ressursStateBuilder(String navn) {
        var navIdentStr = generateNavIdent().navIdent();
        return ressursStateBuilder(navn, navIdentStr);
    }

    public RessursState.RessursStateBuilder ressursStateBuilder(String navn, String navIdentStr) {
        return RessursState.builder()
            .visningsnavn(new Visningsnavn(navn, Optional.empty(), "(" + navIdentStr + ")"))
            .navident(navIdentStr)
            .fid("fid-" + navIdentStr)
            .sektor(Sektor.NAV_STATLIG)
            .personident(navIdentStr.substring(1) + navIdentStr.substring(2))
            .identType(IdentType.STANDARD);
    }
}
