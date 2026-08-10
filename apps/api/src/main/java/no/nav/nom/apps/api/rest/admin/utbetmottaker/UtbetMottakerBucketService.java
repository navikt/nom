package no.nav.nom.apps.api.rest.admin.utbetmottaker;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.Set;

import static no.nav.nom.apps.api.rest.admin.utbetmottaker.UtbetMottakerInnlesningResultat.MatchType;
import static no.nav.nom.apps.api.rest.admin.utbetmottaker.UtbetMottakerInnlesningResultat.MatchStatistikk;
import static no.nav.nom.apps.api.rest.admin.utbetmottaker.UtbetMottakerInnlesningResultat.OtStatistikk;
import static no.nav.nom.apps.api.rest.admin.utbetmottaker.UtbetMottakerInnlesningResultat.OtSlettingStatistikk;

@Service
@Slf4j
public class UtbetMottakerBucketService {

    @Value("${app.google-storage-bucket-name.utbet-mottakere}")
    private String bucketName;

    @Value("${nais.cluster.name:local}")
    private String gsProjectId;

    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    private static final Set<String> OPPDATERBARE_ANSETTELSESTYPER = Set.of("Ekstern", "Tolk", "Lese og sekretærhjelp");

    public UtbetMottakerBucketService(RessursStateRepository ressursStateRepository, OrgtilknytningStateRepository orgtilknytningStateRepository) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    public UtbetMottakerInnlesningResultat lesFraFil(String filnavn) {
        log.info("Starter innlesning av utbetalingsmottakere fra bucket={} fil={}", bucketName, filnavn);

        Storage storage = StorageOptions.newBuilder().setProjectId(gsProjectId).build().getService();

        try (ReadChannel reader = storage.reader(BlobId.of(bucketName, filnavn))) {
            BufferedReader br = new BufferedReader(Channels.newReader(reader, StandardCharsets.UTF_8));
            List<ParsetRad> parseteRader = parseRader(br);

            long antallDuplikater = parseteRader.stream().filter(ParsetRad::duplikat).count();
            long antallUnike = parseteRader.stream().filter(r -> !r.duplikat()).count();

            List<UtbetMottakerInnlesningResultat.DuplikatInfo> duplikater = parseteRader.stream()
                .filter(ParsetRad::duplikat)
                .map(r -> new UtbetMottakerInnlesningResultat.DuplikatInfo(r.ressnr(), fodselsnrPrefix(r.fodselsnr())))
                .toList();

            List<UtbetMottakerRad> mottakere = parseteRader.stream().filter(r -> !r.duplikat()).map(this::berikMedNomData).toList();

            List<UtbetMottakerRad> oppdaterbare = mottakere.stream().filter(UtbetMottakerRad::oppdaterbar).toList();
            List<UtbetMottakerRad> ikkeOppdaterbare = mottakere.stream().filter(r -> !r.oppdaterbar()).toList();

            MatchStatistikk matchStatistikk = new MatchStatistikk(
                count(mottakere, MatchType.RESSNR_OG_FNR),
                count(mottakere, MatchType.KUN_RESSNR_FNR_MISMATCH),
                count(mottakere, MatchType.KUN_FNR_RESSNR_MISMATCH),
                count(mottakere, MatchType.INGEN_MATCH)
            );

            OtSlettingStatistikk otSletting = byggOtSlettingStatistikk(oppdaterbare);

            log.info(
                "Ferdig innlesning: totalt={} unike={} duplikater={} fra fil={}. " + "Match: ressnrOgFnr={} kunRessnr={} kunFnr={} ingenMatch={}. " + "Oppdaterbare={} ikkeOppdaterbare={} otTilSletting(ressurser={} oter={})",
                parseteRader.size(),
                antallUnike,
                antallDuplikater,
                filnavn,
                matchStatistikk.ressnrOgFnr(),
                matchStatistikk.kunRessnrFnrMismatch(),
                matchStatistikk.kunFnrRessnrMismatch(),
                matchStatistikk.ingenMatch(),
                oppdaterbare.size(),
                ikkeOppdaterbare.size(),
                otSletting.antallRessurser(),
                otSletting.antallOter()
            );

            return new UtbetMottakerInnlesningResultat(
                filnavn,
                parseteRader.size(),
                antallUnike,
                antallDuplikater,
                matchStatistikk,
                byggOtStatistikk(oppdaterbare),
                byggOtStatistikk(ikkeOppdaterbare),
                otSletting,
                duplikater,
                mottakere
            );
        } catch (Exception e) {
            log.error("Klarer ikke å lese utbetalingsmottakere fra bucket={} fil={}", bucketName, filnavn, e);
            throw new UtbetMottakerBucketException("Klarer ikke å lese fra bucket: " + e.getMessage(), e);
        }
    }

    private UtbetMottakerRad berikMedNomData(ParsetRad rad) {
        Optional<RessursState> ressnrMatch = finnVedRessnr(rad.ressnr());
        Optional<RessursState> fnrMatch = ressursStateRepository.findByPersonident(rad.fodselsnr());

        RessursState matchetRessurs;
        MatchType matchType;

        if (ressnrMatch.isPresent()) {
            boolean fnrStemmer = fnrMatch.isPresent() && fnrMatch.get().navident().equals(ressnrMatch.get().navident());
            matchType = fnrStemmer ? MatchType.RESSNR_OG_FNR : MatchType.KUN_RESSNR_FNR_MISMATCH;
            matchetRessurs = ressnrMatch.get();
        } else if (fnrMatch.isPresent()) {
            matchType = MatchType.KUN_FNR_RESSNR_MISMATCH;
            matchetRessurs = fnrMatch.get();
        } else {
            return byggRad(rad, MatchType.INGEN_MATCH, null, null, null);
        }
        Collection<OrgtilknytningState> ots = orgtilknytningStateRepository.findByNavident(matchetRessurs.navident());

        return byggRad(rad, matchType, matchetRessurs.navident(), matchetRessurs.sektor(), byggOrgtilknytningInfo(ots));
    }

    private UtbetMottakerRad byggRad(
        ParsetRad rad,
        MatchType matchType,
        String navident,
        no.nav.nom.contract.nom.Sektor sektor,
        UtbetMottakerRad.OrgtilknytningInfo orgtilknytninger
    ) {
        boolean oppdaterbar = OPPDATERBARE_ANSETTELSESTYPER.contains(rad.ansettelsestypeTekst());

        List<String> otFidTilSletting = List.of();
        if (oppdaterbar && orgtilknytninger != null
            && orgtilknytninger.antall() > 0
            && !orgtilknytninger.harAktiveEllerFremtidige()) {
            otFidTilSletting = orgtilknytninger.stater().stream()
                .map(no.nav.nom.contract.nom.state.OrgtilknytningState::fid)
                .toList();
        }

        return new UtbetMottakerRad(
            rad.ressnr(),
            fodselsnrPrefix(rad.fodselsnr()),
            rad.navn(),
            rad.resstypeKode(),
            rad.resstypeTekst(),
            rad.alder(),
            rad.kjonn(),
            rad.ansettelsestypeKode(),
            rad.ansettelsestypeTekst(),
            rad.startdatoNav(),
            rad.stkode(),
            rad.stillingsnummerTekst(),
            rad.orgenhet(),
            rad.orgenhetTekst(),
            false,
            oppdaterbar,
            matchType,
            navident,
            sektor,
            otFidTilSletting,
            orgtilknytninger
        );
    }

    /**
     * Navident-formatet er én stor bokstav + 6 sifre.
     * Ressnr i filen tilsvarer de 6 sifrene, ev. med ledende nuller til 6 sifre.
     */
    private Optional<RessursState> finnVedRessnr(String ressnr) {
        if (ressnr == null || ressnr.isBlank()) {
            return Optional.empty();
        }
        String ressnrPadded = String.format("%6s", ressnr.trim()).replace(' ', '0');
        return ressursStateRepository.getAll()
            .stream()
            .filter(r -> r.navident() != null && r.navident().length() == 7 && r.navident().substring(1).equals(ressnrPadded))
            .findFirst();
    }

    private UtbetMottakerRad.OrgtilknytningInfo byggOrgtilknytningInfo(Collection<OrgtilknytningState> ots) {
        boolean harAktiveEllerFremtidige = ots.stream().anyMatch(this::erAktivEllerFremtidig);

        List<no.nav.nom.contract.nom.Tilknytningtype> tilknytningstyper = ots.stream()
            .map(OrgtilknytningState::orgtilknytningType)
            .filter(t -> t != null)
            .distinct()
            .collect(Collectors.toList());

        List<no.nav.nom.contract.nom.Sektor> sektorer = ots.stream()
            .map(OrgtilknytningState::sektor)
            .filter(s -> s != null)
            .distinct()
            .collect(Collectors.toList());

        return new UtbetMottakerRad.OrgtilknytningInfo(ots.size(), harAktiveEllerFremtidige, tilknytningstyper, sektorer, List.copyOf(ots));
    }

    private boolean erAktivEllerFremtidig(OrgtilknytningState ot) {
        return ot.gyldigTom() == null || ot.gyldigTom().isAfter(LocalDate.now());
    }

    private OtSlettingStatistikk byggOtSlettingStatistikk(List<UtbetMottakerRad> oppdaterbare) {
        List<UtbetMottakerRad> medFider = oppdaterbare.stream()
            .filter(r -> !r.otFidTilSletting().isEmpty())
            .toList();
        long antallOter = medFider.stream()
            .mapToLong(r -> r.otFidTilSletting().size())
            .sum();
        return new OtSlettingStatistikk(medFider.size(), antallOter);
    }

    private OtStatistikk byggOtStatistikk(List<UtbetMottakerRad> rader) {
        long medAktiveEllerFremtidige = rader.stream().filter(r -> r.orgtilknytninger() != null && r.orgtilknytninger().harAktiveEllerFremtidige()).count();
        long utenOrgtilknytning = rader.stream().filter(r -> r.orgtilknytninger() == null || r.orgtilknytninger().antall() == 0).count();
        long medKunHistoriske = rader.size() - medAktiveEllerFremtidige - utenOrgtilknytning;
        return new OtStatistikk(medAktiveEllerFremtidige, medKunHistoriske, utenOrgtilknytning);
    }

    private String fodselsnrPrefix(String fodselsnr) {
        if (fodselsnr == null) {
            return "";
        }
        return fodselsnr.substring(0, Math.min(6, fodselsnr.length()));
    }

    private long count(List<UtbetMottakerRad> mottakere, MatchType type) {
        return mottakere.stream().filter(m -> m.matchType() == type).count();
    }

    List<ParsetRad> parseRader(BufferedReader reader) throws IOException {
        List<ParsetRad> rader = new ArrayList<>();
        Set<String> setteFodselsnr = new HashSet<>();

        String linje;
        boolean forsteRad = true;

        while ((linje = reader.readLine()) != null) {
            if (forsteRad) {
                forsteRad = false;
                continue; // hopp over header
            }

            if (linje.isBlank()) {
                continue;
            }

            String[] kolonner = linje.split(";", -1);
            if (kolonner.length < 15) {
                log.warn("Ugyldig rad (for få kolonner): {}", linje.substring(0, Math.min(6, linje.length())));
                continue;
            }

            String fodselsnr = kolonner[1].trim();
            boolean erDuplikat = !setteFodselsnr.add(fodselsnr);

            rader.add(new ParsetRad(
                trim(kolonner[0]),   // Ressnr
                fodselsnr,           // Fødselsnr (internt — eksponeres kun som prefix)
                trim(kolonner[3]),   // Ressnr(T) — navn
                trim(kolonner[4]),   // Resstype kode
                trim(kolonner[5]),   // Resstype(T) tekst
                trim(kolonner[6]),   // Alder
                trim(kolonner[7]),   // Kjønn
                trim(kolonner[8]),   // Ansettelsestype kode
                trim(kolonner[9]),   // Ansettelsestype(T) tekst
                trim(kolonner[10]),  // Startdato Nav
                trim(kolonner[11]),  // Stkode
                trim(kolonner[12]),  // Stillingsnummer(T)
                trim(kolonner[13]),  // Orgenhet
                trim(kolonner[14]),  // Orgenhet(T)
                erDuplikat
            ));
        }

        return rader;
    }

    /**
     * Intern record for parsing — holder fullt fødselsnr, eksponeres aldri utad.
     */
    record ParsetRad(
        String ressnr,
        String fodselsnr,
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
        boolean duplikat
    ) {}

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
