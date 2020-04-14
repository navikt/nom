package no.nav.nom.apps.nom.ressurser.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.common.LeaderElection;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("!local")
@Slf4j
public class DvhConsumer {

    @Value("${dvh.endpoint}")
    private String dvhEndpoint;
    private final RessursRepository ressursRepository;
    private final OffsetRepository offsetRepository;
    private final LeaderElection leaderElection;

    private final RestTemplate restTemplate = new RestTemplate();
    private final long incrementValue = 1000L;

    @Scheduled(cron = "${app.scheduledTasks.dvhConsumer}")
    public void dvhScheduler(){
        log.info("Oppdaterer fra datavarehus..");
        while (hentBulkOppdatering() == incrementValue);
        log.info("Oppdatering fra datavarehus ferdig.");
    }

    @Transactional(value = "transactionManager")
    public int hentBulkOppdatering(){
        if(leaderElection.isLeader()) {
            Offset dvhOffset = offsetRepository.findByOffsetName("DVH_OFFSET");
            String url = dvhEndpoint + "?in_sekvensnummer_fra=" + dvhOffset.getOffset() + "&in_sekvensnummer_til=" + (dvhOffset.getOffset() + incrementValue);
            log.info("Leser fra sekvens {} til sekvens {}", dvhOffset.getOffset(), dvhOffset.getOffset() + incrementValue);
            DvhResultWrapper result = restTemplate.getForObject(url, DvhResultWrapper.class);

            if (result != null && result.getAnsatteResultList() != null && !result.getAnsatteResultList().isEmpty()) {
                List<DvhRessursDTO> resultRessurs = result.getAnsatteResultList();

                resultRessurs.sort(Comparator.comparingLong(DvhRessursDTO::getOffset));
                resultRessurs.stream()
                        .filter(ressurs -> {
                            if (ressurs.getNavident() != null) {
                                log.info("Gylding hendelse fra dvh med sekvensnr: {}", ressurs.getOffset());
                                return true;
                            } else {
                                log.warn("Ugyldig hendelse fra dvh, navident mangler for sekvensnr: {}", ressurs.getOffset());
                                return false;
                            }
                        })
                        .map(DvhConsumer::converted)
                        .forEach(ressurs ->
                                ressursRepository.findByPersonident(ressurs.getPersonident())
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
                                        )
                        );
                dvhOffset.setOffset(resultRessurs.get(resultRessurs.size() - 1).getOffset());
                offsetRepository.save(dvhOffset);

                return result.getAnsatteResultList().size();
            }
        }
        return 0;
    }

    private static Ressurs converted(DvhRessursDTO dvhRessursDTO){
        LocalDate sluttDato = dvhRessursDTO.getAgressoSluttDato() == null ? dvhRessursDTO.getRemedySluttDato() : dvhRessursDTO.getAgressoSluttDato();
        if(sluttDato != null && sluttDato.isEqual(LocalDate.parse("2099-12-31"))){
            sluttDato = null;
        }

        return Ressurs.builder()
                .navident(dvhRessursDTO.getNavident())
                .personident(dvhRessursDTO.getPersonIdent())
                .fornavn(dvhRessursDTO.getFornavn())
                .etternavn(dvhRessursDTO.getEtternavn())
                .epost(dvhRessursDTO.getEpost())
                .ressurstype(convertedRessursType(dvhRessursDTO.getRessursTypeKode(), dvhRessursDTO.getAnsattType()))
                .startdato(dvhRessursDTO.getRemedyStartDato())
                .sluttdato(sluttDato)
                .endretDato(LocalDate.now())
                .produced(false)
                .build();
    }

    private static Ressurstype convertedRessursType(String ressurstypeKode, String ansattType){
        if(ressurstypeKode != null) {
            switch (ressurstypeKode.toUpperCase()) {
                case "F":
                case "K":
                case "M":
                case "N":
                    return Ressurstype.INTERN;
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
        else {
            return ansattType.equals("NAV Kommunalt") || ansattType.equals("NAV Statlig") ? Ressurstype.INTERN : (ansattType.equals("Eksternt") ? Ressurstype.INTERN : null);
        }
    }
}
