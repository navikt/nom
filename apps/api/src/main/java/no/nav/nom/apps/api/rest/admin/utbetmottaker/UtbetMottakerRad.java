package no.nav.nom.apps.api.rest.admin.utbetmottaker;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.util.List;

/**
 * En rad fra utbetalingsmottaker-filen, beriket med matchet NOM-ressurs og orgtilknytninger.
 *
 * @param duplikat         true dersom fødselsnr allerede er sett tidligere i samme innlesning
 * @param oppdaterbar      true dersom ansettelsestypeTekst er Ekstern, Tolk eller Lese og sekretærhjelp (aldri true for duplikater)
 * @param matchType        hvordan raden ble matchet mot NOM (null for duplikater)
 * @param navident         matchet navident i NOM (null ved ingen match)
 * @param sektor           sektor på matchet ressurs (null ved ingen match)
 * @param orgtilknytninger orgtilknytning-info for matchet ressurs (null ved ingen match)
 * @param otFidTilSletting fid-er for alle OT-er som skal slettes — kun satt for oppdaterbare rader med kun historiske OT-er
 */
public record UtbetMottakerRad(
    String ressnr,
    String fodselsnrPrefix,
    String navn,
    String resstypeKode,
    String resstypeTekst,
    String alder,
    String kjonn,
    String ansettelsestypeKode,
    String ansettelsestypeTekst,
    String startdatoNav,
    String stkode,
    String stillingsnummerTekst,
    String orgenhet,
    String orgenhetTekst,
    boolean duplikat,
    boolean oppdaterbar,
    UtbetMottakerInnlesningResultat.MatchType matchType,
    String navident,
    Sektor sektor,
    List<String> otFidTilSletting,
    OrgtilknytningInfo orgtilknytninger
) {
    public record OrgtilknytningInfo(
        int antall,
        boolean harAktiveEllerFremtidige,
        List<Tilknytningtype> tilknytningstyper,
        List<Sektor> sektorer,
        List<OrgtilknytningState> stater
    ) {
    }
}
