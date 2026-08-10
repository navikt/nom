package no.nav.nom.apps.api.rest.admin.utbetmottaker;

import java.util.List;

public record UtbetMottakerInnlesningResultat(
    String filnavn,
    long antallTotalt,
    long antallUnike,
    long antallDuplikater,
    MatchStatistikk matchStatistikk,
    OtStatistikk oppdaterbare,
    OtStatistikk ikkeOppdaterbare,
    OtSlettingStatistikk otSletting,
    List<DuplikatInfo> duplikater,
    List<UtbetMottakerRad> mottakere
) {

    public record DuplikatInfo(
        String ressnr,
        String fodselsnrPrefix
    ) {}

    public record MatchStatistikk(
        long ressnrOgFnr,
        long kunRessnrFnrMismatch,
        long kunFnrRessnrMismatch,
        long ingenMatch
    ) {}

    public enum MatchType {
        /**
         * Treff på både ressursnummer (navident-sifre) og fødselsnummer
         */
        RESSNR_OG_FNR,
        /**
         * Treff på ressursnummer, men fødselsnummer stemmer ikke med personident i NOM
         */
        KUN_RESSNR_FNR_MISMATCH,
        /**
         * Treff på fødselsnummer, men navident-sifrene matcher ikke ressursnummer
         */
        KUN_FNR_RESSNR_MISMATCH,
        /**
         * Ingen treff verken på ressursnummer eller fødselsnummer
         */
        INGEN_MATCH
    }

    /**
     * Opptelling av orgtilknytningsstatus for en gruppe mottakere.
     *
     * @param antallMedAktiveEllerFremtidige har minst én aktiv eller fremtidig OT
     * @param antallMedKunHistoriske         har OT-er, men alle er historiske
     * @param antallUtenOrgtilknytning       har ingen OT-er i det hele tatt
     */
    public record OtStatistikk(
        long antallMedAktiveEllerFremtidige,
        long antallMedKunHistoriske,
        long antallUtenOrgtilknytning
    ) {}

    /**
     * Oppsummering av OT-er som er kandidater for sletting —
     * dvs. oppdaterbare rader som kun har historiske OT-er.
     *
     * @param antallRessurser antall unike ressurser (mottakere) med OT-er til sletting
     * @param antallOter      totalt antall OT-fid-er på tvers av disse ressursene
     */
    public record OtSlettingStatistikk(
        long antallRessurser,
        long antallOter
    ) {}
}
