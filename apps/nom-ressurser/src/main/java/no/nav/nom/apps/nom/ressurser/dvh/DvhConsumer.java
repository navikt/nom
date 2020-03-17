package no.nav.nom.apps.nom.ressurser.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Profile("!local")
@Component
@RequiredArgsConstructor
@Slf4j
public class DvhConsumer {

    @Value("${dvh.endpoint}")
    private String dvhEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();
    private final RessursRepository ressursRepository;
    private final OffsetRepository offsetRepository;

    @Scheduled(fixedDelay = 5000)
    @Transactional(value = "transactionManager")
    public void hentBulkOppdatering(){
        Offset dvhOffset = offsetRepository.findByOffsetName("DVH_OFFSET");
        String url = dvhEndpoint + "?in_sekvensnummer=" + dvhOffset.getOffset() + "&in_sekvensnummer_til=" +dvhOffset.getIncrementedValue();
        DvhResultWrapper result = restTemplate.getForObject(url, DvhResultWrapper.class);

        if(result.getAnsatteResultList() != null) {
            result.getAnsatteResultList().stream()
                    .map(DvhRessursDTO::getOffset)
                    .max(Long::compare)
                    .ifPresent(offset -> offsetRepository.save(dvhOffset.setOffset(offset)));

            result.getAnsatteResultList().stream()
                    .map(DvhConsumer::converted)
                    .forEach(ressurs ->
                        ressursRepository.findByPersonident(ressurs.getPersonident())
                                .map(eksisterende -> {
                                    ressurs.setId(eksisterende.getId());
                                    ressurs.setOpprettetDato(eksisterende.getOpprettetDato());
                                    ressurs.setEndretDato(LocalDate.now());
                                    return ressursRepository.save(ressurs);
                                })
                                .orElseGet(() -> {
                                    ressurs.setOpprettetDato(LocalDate.now());
                                    ressurs.setEndretDato(LocalDate.now());
                                    return ressursRepository.save(ressurs);
                                })
                    );
        }
    }

    private static Ressurs converted(DvhRessursDTO dvhRessursDTO){
        return Ressurs.builder()
                .navIdent(dvhRessursDTO.getNavIdent())
                .personident(dvhRessursDTO.getPersonIdent())
                .fornavn(dvhRessursDTO.getFornavn())
                .etternavn(dvhRessursDTO.getEtternavn())
                .epost(dvhRessursDTO.getEpost())
                .ressurstype(Ressurstype.EKSTERN)
                .startdato(dvhRessursDTO.getRemedyStartDato())
                .sluttdato(dvhRessursDTO.getAgressoSluttDato() == null ? dvhRessursDTO.getRemedySluttDato() : dvhRessursDTO.getAgressoSluttDato())
                .endretDato(LocalDate.now())
                .build();
    }
}
