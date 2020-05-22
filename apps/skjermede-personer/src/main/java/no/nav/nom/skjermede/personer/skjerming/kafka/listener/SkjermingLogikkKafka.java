package no.nav.nom.skjermede.personer.skjerming.kafka.listener;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.skjermede.personer.skjerming.Kilde;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class SkjermingLogikkKafka {

    @Autowired
    private SkjermetPersonRepository repository;

    @Value("${maanderPlusSkjerming}")
    private int mnderSkjermetPlus;

    /*
1. Hendelse kommer inn.
2. Hvis hendelse er av type INTERN, fortsett, hvis ikke ikke gjør noe.
3. Sett gyldig til på ansatt sin familie. (+ 6mnder hvis den ikke er null)
4. Sjekk om aktiv skjerming finnes på personen.
    4.1.a  Hvis aktiv skjerming finnes, hent ut denne fra database.
    4.1.b  Hvis personen har tilknyttet ressurs, sjekk om skjermetHendelse har sluttdato etter idag.
    4.1.b.1  Dersom dette stemmer, avslutt nåværende skjerming og lag ny. Nåværende skjerming's skjermetTil er lik ny skjerming's skjermetFra dato.
    4.1.b.2  Dersom dette ikke stemmer, ikke gjør noe. Oppdatering på gammel ansettelsesforhold skal ikke påvirke nåværende skjerming, med mindre forholdet blir aktivt igjen.
    4.1.c  Hvis personen ikke har tilknyttet ressurs, oppdater allerede eksisterende rad. Hvis personen har en sluttdato, avslutt skjermingen (+6mnder).
    4.2.a  Hvis aktiv skjerming ikke finnes, sjekk om den ansatte opphørte sin forrige skjerming (endret_av Kilde.BRUKER).
    4.2.a.1  Dersom dette stemmer (skjerming har blitt opphørt tidligere), ikke gjør noe.
    4.2.a.2  Dersom dette ikke stemmer, lag ny skjerming for personen. SkjermingFra kan tidligst være nå, og er sluttdato allerede satt settes også skjermetTil (+6mndr).
 */
    public void handterAnsattHendelse(SkjermetPerson skjermetHendelse, String navident) {
                // gyldig til på familie. (vi kan ikke alltid anta at en ansatt er skjermet, så må derfor gjøres her)
                List<SkjermetPerson> skjermetFamilie = repository.findAllByTilknyttetRessursAndValid(navident);
                if(!skjermetFamilie.isEmpty()){
                    skjermetFamilie.forEach(skjermetPerson -> {
                        LocalDateTime sluttDato = skjermetHendelse.getSkjermetTil() != null ? skjermetHendelse.getSkjermetTil().plusMonths(mnderSkjermetPlus) : skjermetHendelse.getSkjermetTil();
                        if(!Objects.equals(sluttDato, skjermetPerson.getSkjermetTil())){
                            skjermetPerson.setSkjermetTil(sluttDato);
                            skjermetPerson.setKafkaProduced(false);
                            skjermetPerson.setEndretAv(Kilde.SERVICEBRUKER);
                            skjermetPerson.setEndretDato(LocalDateTime.now());
                            log.info("Skjermingen for id: {} med tilknyttetressurs: {} har fått satt skjermet til dato.", skjermetPerson.getId(), navident);
                        }
                    });
                    repository.saveAll(skjermetFamilie);
                }

                repository.getSkjermetPersonByPersonident(skjermetHendelse.getPersonident())
                        .ifPresentOrElse(
                                (eksisterende) -> {
                                    // Personen har vært skjermet tidligere via familie eller har videreført skjermingen
                                    if (eksisterende.getTilknyttetRessurs() != null) {
                                        if (skjermetHendelse.getSkjermetTil() == null || skjermetHendelse.getSkjermetTil().isAfter(LocalDateTime.now())) {
                                            eksisterende.setSkjermetTil(skjermetHendelse.getSkjermetFra());
                                            eksisterende.setEndretDato(LocalDateTime.now());
                                            eksisterende.setEndretAv(Kilde.SERVICEBRUKER);
                                            repository.save(eksisterende);

                                            skjermetHendelse.setSkjermetTil(skjermetHendelse.getSkjermetTil() != null ? skjermetHendelse.getSkjermetTil().plusMonths(mnderSkjermetPlus) : skjermetHendelse.getSkjermetTil());
                                            nySkjerming(skjermetHendelse);
                                            log.info("Skjerming overført til ny rad for ansettelse for ident: {}", navident);
                                        }
                                    }
                                    // Personen har en endring i person opplysninger eller fått utsatt/endret sluttdatoen sin,
                                    // eller starter igjen innenfor mnderSkjermetPlus.
                                    else {
                                        eksisterende.setEndretDato(LocalDateTime.now());
                                        eksisterende.setKafkaProduced(false);
                                        eksisterende.setEndretAv(Kilde.SERVICEBRUKER);
                                        eksisterende.setFornavn(skjermetHendelse.getFornavn());
                                        eksisterende.setEtternavn(skjermetHendelse.getEtternavn());
                                        eksisterende.setSkjermetTil(skjermetHendelse.getSkjermetTil() != null ? skjermetHendelse.getSkjermetTil().plusMonths(mnderSkjermetPlus) : skjermetHendelse.getSkjermetTil());
                                        repository.save(eksisterende);
                                        log.info("Ny oppdatering fra aapen-nom-ressurs-v1 for ident: {}", navident);
                                    }
                                },
                                () -> {
                                    // Hvis en ansatt har vært skjermet tidligere, men avskjermet seg via nav.no skal den ikke skjermes igjen.
                                    // Skjermes kun ved et nytt ansettelsesforhold som ikke har blitt avskjermet tidligere.
                                    List<SkjermetPerson> historiskSkjermet = repository.findAllByPersonident(skjermetHendelse.getPersonident());
                                    if(historiskSkjermet == null || historiskSkjermet.stream().filter(skjermetPerson -> skjermetPerson.getTilknyttetRessurs() == null ).max(Comparator.comparing(SkjermetPerson::getEndretDato)).filter(skjermetPerson -> skjermetPerson.getEndretAv() == Kilde.BRUKER).isEmpty()){
                                        skjermetHendelse.setSkjermetTil(skjermetHendelse.getSkjermetTil() != null ? skjermetHendelse.getSkjermetTil().plusMonths(mnderSkjermetPlus) : skjermetHendelse.getSkjermetTil());
                                        nySkjerming(skjermetHendelse);
                                        log.info("Ny skjerming opprettet fra aapen-nom-ressurs-v1 for ident: {}", navident);
                                    }
                                }
                        );
        }

        private void nySkjerming(SkjermetPerson nySkjermetPerson){
            nySkjermetPerson.setEndretDato(LocalDateTime.now());
            nySkjermetPerson.setOpprettetDato(LocalDateTime.now());
            nySkjermetPerson.setOpprettetAv(Kilde.SERVICEBRUKER);
            nySkjermetPerson.setEndretAv(Kilde.SERVICEBRUKER);
            nySkjermetPerson.setKafkaProduced(false);
            repository.save(nySkjermetPerson);
        }
}
