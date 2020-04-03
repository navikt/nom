package no.nav.nom.skjermede.personer.skjerming;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@AllArgsConstructor
@Slf4j
public class SkjermingLogikkKafka {

    private final SkjermetPersonRepository repository;

    @Value("${maanderPlusSkjerming}")
    private final int mnderSkjermetPlus = 6;


    /*
1. Hendelse kommer inn.
2. Hvis hendelse er av type INTERN, fortsett, hvis ikke ikke gjør noe.
3. Sett gyldig til på ansatt sin familie. (+ 6mnder hvis den ikke er null)
4. Sjekk om aktiv skjerming finnes på personen.
    4.1.a. Hvis aktiv skjerming finnes, hent ut denne fra database.
    4.1.b. Hvis ulik start/skjermet FOM dato, avslutt gammel skjerming, og lag ny.
    4.1.c. Hvis personen har en sluttdato, avslutt skjermingen (+6mnder)
    4.1.d. Hvis ikke 4.1.b og 4.1.c inntreffer, oppdater allerede eksisterende rad.
    4.2.a. Hvis aktiv skjerming ikke finnes, sjekk om personen noen gang har vært skjermet med lik start/skjermet_fom dato.
    4.2.b  Hvis personen har vært skjermet med lik start/skjermet_fom dato fra før av (ikke aktiv skjerming), så gjøres ingenting mer.
    4.2.c  Hvis personen ikke har vært skjermet med lik start/skjermet_fom dato fra før av, lag ny skjerming for personen.

 */
    void handterAnsattHendelse(SkjermetPerson skjermetHendelse, String navident) {
                // gyldig til på familie. (vi kan ikke alltid anta at en ansatt er skjermet, så må derfor gjøres her)
                List<SkjermetPerson> skjermetFamilie = repository.findAllByTilknyttetRessursAndValid(navident);
                if(skjermetFamilie != null){
                    skjermetFamilie.forEach(skjermetPerson -> {
                        LocalDate sluttDato = skjermetHendelse.getSkjermetTom() != null ? skjermetHendelse.getSkjermetTom().plusMonths(mnderSkjermetPlus) : skjermetHendelse.getSkjermetTom();
                        if(!Objects.equals(sluttDato, skjermetPerson.getSkjermetTom())){
                            skjermetPerson.setSkjermetTom(sluttDato);
                            skjermetPerson.setEndretDato(LocalDateTime.now());
                            log.info("Skjermingen for id: {} med tilknyttetressurs: {} har fått satt skjermet til og med dato.", skjermetPerson.getId(), navident);
                        }
                    });
                    repository.saveAll(skjermetFamilie);
                }

                repository.getSkjermetPersonByPersonident(skjermetHendelse.getPersonident())
                        .ifPresentOrElse(
                                (eksisterende) -> {
                                    // Personen har en endring i person opplysninger eller fått utsatt/endret sluttdatoen sin,
                                    // eller starter igjen innenfor mnderSkjermetPlus.
                                    if(skjermetHendelse.getSkjermetFom().equals(eksisterende.getSkjermetFom()) || eksisterende.getSkjermetTom().isBefore(skjermetHendelse.getSkjermetFom())){
                                        eksisterende.setEndretDato(LocalDateTime.now());
                                        eksisterende.setFornavn(skjermetHendelse.getFornavn());
                                        eksisterende.setEtternavn(skjermetHendelse.getEtternavn());
                                        eksisterende.setSkjermetTom(skjermetHendelse.getSkjermetTom() != null ? skjermetHendelse.getSkjermetTom().plusMonths(mnderSkjermetPlus) : skjermetHendelse.getSkjermetTom());
                                        repository.save(eksisterende);
                                        log.info("Ny oppdatering fra aapen-nom-ressurs-v1 for ident: {}", navident);
                                    }
                                    // Personen har vært skjermet tidligere (muligens via familie):
                                    else{
                                        eksisterende.setSkjermetTom(LocalDate.now());
                                        eksisterende.setEndretDato(LocalDateTime.now());
                                        repository.save(eksisterende);

                                        nySkjerming(skjermetHendelse);
                                        log.info("Skjerming overført til ny rad for ansettelse for ident: {}", navident);
                                    }
                                },
                                () -> {
                                    // Hvis en person har vært skjermet tidligere, men avskjermet seg via nav.no skal det ikke skjermes igjen.
                                    // Skjermes kun ved et nytt ansettelsesforhold som ikke har blitt skjermet tidligere.
                                    List<SkjermetPerson> historiskSkjermet = repository.findAllByPersonident(skjermetHendelse.getPersonident());
                                    if(historiskSkjermet == null || historiskSkjermet.stream().noneMatch(historisk -> historisk.getSkjermetFom().equals(skjermetHendelse.getSkjermetFom()))){
                                        nySkjerming(skjermetHendelse);
                                        log.info("Ny skjerming opprettet fra aapen-nom-ressurs-v1 for ident: {}", navident);
                                    }
                                }
                        );
        }

        private void nySkjerming(SkjermetPerson nySkjermetPerson){
            nySkjermetPerson.setEndretDato(LocalDateTime.now());
            nySkjermetPerson.setOpprettetDato(LocalDateTime.now());
            repository.save(nySkjermetPerson);
        }
}
