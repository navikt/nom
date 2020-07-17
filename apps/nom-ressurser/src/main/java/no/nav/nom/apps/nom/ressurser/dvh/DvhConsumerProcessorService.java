package no.nav.nom.apps.nom.ressurser.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DvhConsumerProcessorService {

    private static final LocalDate D2099 = LocalDate.of(2099, 12, 31);
    private final RessursRepository ressursRepository;

    public void processRessurser(final List<DvhRessursDTO> dvhRessurser) {
        dvhRessurser.sort(Comparator.comparingLong(DvhRessursDTO::getOffset));
        dvhRessurser.stream()
                .filter(ressurs -> {
                    if (ressurs.getNavident() != null) {
                        log.info("Gylding hendelse fra dvh med sekvensnr: {}", ressurs.getOffset());
                        return true;
                    } else {
                        log.warn("Ugyldig hendelse fra dvh, navident mangler for sekvensnr: {}", ressurs.getOffset());
                        return false;
                    }
                })
                .map(this::converted)
                .forEach(this::handleRessurs);
    }

    private Ressurs converted(final DvhRessursDTO dvhRessursDTO) {
        return Ressurs.builder()
                .navident(dvhRessursDTO.getNavident())
                .personident(dvhRessursDTO.getPersonIdent())
                .fornavn(dvhRessursDTO.getFornavn())
                .etternavn(dvhRessursDTO.getEtternavn())
                .epost(dvhRessursDTO.getEpost())
                .ressurstype(convertedRessursType(dvhRessursDTO.getRessursTypeKode(), dvhRessursDTO.getAnsattType()))
                .startdato(dvhRessursDTO.getRemedyStartDato())
                .sluttdato(getSluttDato(dvhRessursDTO))
                .endretDato(LocalDate.now())
                .produced(false)
                .build();
    }

    private void handleRessurs(final Ressurs ressurs) {
        ressursRepository
                .findByPersonident(ressurs.getPersonident())
                .ifPresentOrElse(
                        (eksisterende) -> {
                            ressurs.setId(eksisterende.getId());
                            ressurs.setOpprettetDato(eksisterende.getOpprettetDato());
                            ressurs.setEndretDato(LocalDate.now());
                            ressursRepository.save(ressurs);
                        },
                        () -> {
                            ressurs.setOpprettetDato(LocalDate.now());
                            ressurs.setEndretDato(LocalDate.now());
                            ressursRepository.save(ressurs);
                        }
                );
    }

    private LocalDate getSluttDato(final DvhRessursDTO dvhRessursDTO) {
        if (dvhRessursDTO.getRessursTypeKode().equals("E") ||
            Arrays.asList("Eksternt", "Annen Statlig").contains(dvhRessursDTO.getAnsattType())) {
            return dvhRessursDTO.getRemedySluttDato();
        }

        if (dvhRessursDTO.getAnsattType().equals("NAV Kommunalt")) {
            return dvhRessursDTO.getPermittert().equals("JA")
                    ? getPersistedSluttDato(dvhRessursDTO.getNavident()) : dvhRessursDTO.getRemedySluttDato();
        }

        return dvhRessursDTO.getPermittert().equalsIgnoreCase("JA")
                ? getPersistedSluttDato(dvhRessursDTO.getNavident())
                : dvhRessursDTO.getRemedySluttDato().equals(D2099) ? dvhRessursDTO.getAgressoSluttDato() : dvhRessursDTO.getRemedySluttDato();
    }

    private LocalDate getPersistedSluttDato(final String navident) {
        return ressursRepository.getGyldigRessursByNavident(navident).map(Ressurs::getSluttdato).orElse(null);
    }

    private Ressurstype convertedRessursType(final String ressurstypeKode, final String ansattType) {
        if (ansattType.equals("Annen Statlig")) {
            return Ressurstype.ANNEN_STAT;
        }

        if (ressurstypeKode != null) {
            switch (ressurstypeKode.toUpperCase()) {
                case "F":
                case "K":
                case "M":
                case "N":
                    return ansattType.equals("Eksternt") ? Ressurstype.EKSTERN : Ressurstype.INTERN;
                case "E":
                case "I":
                case "L":
                case "O":
                case "P":
                case "S":
                case "T":
                case "V":
                case "Y":
                    return Ressurstype.EKSTERN;
                default:
                    return null;
            }
        }

        return ansattType.equals("NAV Kommunalt") || ansattType.equals("NAV Statlig")
                ? Ressurstype.INTERN : (ansattType.equals("Eksternt") ? Ressurstype.EKSTERN : null);
    }
}
