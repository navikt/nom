package no.nav.nom.skjermede.personer.skjerming.kafka.listener;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.dto.Ressurstype;
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
2. Hvis hendelse er av type INTERN:
    2.1. Sett gyldig til på ansatt sin familie. (+ 6mnder hvis den ikke er null)
    2.2. Sjekk om aktiv skjerming finnes på personen.
        2.2.1.a  Hvis aktiv skjerming finnes, hent ut denne fra database.
        2.2.1.b  Hvis personen har tilknyttet ressurs, sjekk om skjermetHendelse har sluttdato etter idag.
        2.2.1.b.1  Dersom dette stemmer, avslutt nåværende skjerming og lag ny. Nåværende skjerming's skjermetTil er lik ny skjerming's skjermetFra dato.
        2.2.1.b.2  Dersom dette ikke stemmer, ikke gjør noe. Oppdatering på gammel ansettelsesforhold skal ikke påvirke nåværende skjerming, med mindre forholdet blir aktivt igjen.
        2.2.1.c  Hvis personen ikke har tilknyttet ressurs, oppdater allerede eksisterende rad. Hvis personen har en sluttdato, avslutt skjermingen (+6mnder).
        2.2.2.a  Hvis aktiv skjerming ikke finnes, sjekk om den ansatte opphørte sin forrige skjerming (endret_av Kilde.BRUKER).
        2.2.2.a.1  Dersom dette stemmer (skjerming har blitt opphørt tidligere), ikke gjør noe.
        2.2.2.a.2  Dersom dette ikke stemmer, lag ny skjerming for personen. SkjermingFra kan tidligst være nå, og er sluttdato allerede satt settes også skjermetTil (+6mndr).
3. Hvis hendelse er av type EKSTERN:
    3.1. Sett gyldig til (+ 6mnder) på eksisterende skjermet familie hvis det er noen som har skjermet_til = null eller skjermet_til etter 6mnder fra localdatetime.now.
    3.2. Sett gyldig til (+ 6mnder) på eksisterende skjerming for ansatt hvis det er noen skjermet_til = null eller skjermet_til etter 6mnder fra localdatetime.now.
    3.3. Ignorer hendelsen videre.

Merknader:
- Startdato kan aldri være i fremtiden.
- SkjermetFra blir alltid satt til nå.
- Sluttdato kan være både i fortiden og fremtiden, og null.
- Dersom en tidligere ansatt har videreført skjerming, og så begynner igjen: Vil skjermingen som familie bli avsluttet og ny skjerming som ansatt blir laget.
- Tilsvarende skjer for en person som har vært skjermet som familie før, og så begynner å jobbe i NAV.
 */
    public void handterAnsattHendelse(SkjermetPerson skjermetHendelse, String navident, Ressurstype ressurstype) {
                // gyldig til på familie. (vi kan ikke alltid anta at en ansatt er skjermet, så må derfor gjøres her)
                List<SkjermetPerson> skjermetFamilie = repository.findAllByTilknyttetRessursAndValid(navident);
                if(!skjermetFamilie.isEmpty()){
                    skjermetFamilie.forEach(skjermetPerson -> {
                        if(ressurstype == Ressurstype.INTERN) {
                            LocalDateTime sluttDato = skjermetHendelse.getSkjermetTil() != null ? skjermetHendelse.getSkjermetTil().plusMonths(mnderSkjermetPlus) : skjermetHendelse.getSkjermetTil();
                            if (!Objects.equals(sluttDato, skjermetPerson.getSkjermetTil())) {
                                skjermetPerson.setSkjermetTil(sluttDato);
                                skjermetPerson.setKafkaProduced(false);
                                skjermetPerson.setEndretAv(Kilde.SERVICEBRUKER);
                                skjermetPerson.setEndretDato(LocalDateTime.now());
                                log.info("Skjermingen for id: {} med tilknyttetressurs: {} har fått satt skjermet til dato.", skjermetPerson.getId(), navident);
                            }
                        }
                        // Hvis ekstern har skjermet familie fra tidligere internt forhold:
                        else {
                            if(skjermetPerson.getSkjermetTil() == null || skjermetPerson.getSkjermetTil().isAfter(LocalDateTime.now().plusMonths(mnderSkjermetPlus))) {
                                skjermetPerson.setEndretDato(LocalDateTime.now());
                                skjermetPerson.setKafkaProduced(false);
                                skjermetPerson.setSkjermetTil(LocalDateTime.now().plusMonths(mnderSkjermetPlus));
                                skjermetPerson.setEndretAv(Kilde.SERVICEBRUKER);
                                log.info("Ansatt gått over til å bli ekstern og skjerming oppheves for familien til ident: {}", navident);
                            }
                        }
                    });
                    repository.saveAll(skjermetFamilie);
                }

                repository.getSkjermetPersonByPersonident(skjermetHendelse.getPersonident())
                        .ifPresentOrElse(
                                (eksisterende) -> {
                                    // Hvis ekstern har skjerming fra tidligere internt forhold:
                                    if(ressurstype == Ressurstype.EKSTERN){
                                        if(eksisterende.getSkjermetTil() == null || eksisterende.getSkjermetTil().isAfter(LocalDateTime.now().plusMonths(mnderSkjermetPlus))) {
                                            eksisterende.setEndretDato(LocalDateTime.now());
                                            eksisterende.setKafkaProduced(false);
                                            eksisterende.setSkjermetTil(LocalDateTime.now().plusMonths(mnderSkjermetPlus));
                                            eksisterende.setEndretAv(Kilde.SERVICEBRUKER);
                                            repository.save(eksisterende);
                                            log.info("Ansatt gått over til å bli ekstern og skjerming oppheves for ident: {}", navident);
                                        }
                                    }
                                    // Personen har vært skjermet tidligere via familie eller har videreført skjermingen
                                    else if (eksisterende.getTilknyttetRessurs() != null) {
                                        if (skjermetHendelse.getSkjermetTil() == null || skjermetHendelse.getSkjermetTil().isAfter(LocalDateTime.now())) {
                                            eksisterende.setSkjermetTil(skjermetHendelse.getSkjermetFra());
                                            eksisterende.setEndretDato(LocalDateTime.now());
                                            eksisterende.setEndretAv(Kilde.SERVICEBRUKER);
                                            repository.save(eksisterende);

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
                                    if(ressurstype == Ressurstype.INTERN) {
                                        // Hvis en ansatt har vært skjermet tidligere, men avskjermet seg via nav.no skal den ikke skjermes igjen.
                                        // Skjermes kun ved et nytt ansettelsesforhold som ikke har blitt avskjermet tidligere.
                                        List<SkjermetPerson> historiskSkjermet = repository.findAllByPersonident(skjermetHendelse.getPersonident());
                                        if (historiskSkjermet == null || historiskSkjermet.stream().filter(skjermetPerson -> skjermetPerson.getTilknyttetRessurs() == null).max(Comparator.comparing(SkjermetPerson::getEndretDato)).filter(skjermetPerson -> skjermetPerson.getEndretAv() != Kilde.SERVICEBRUKER).isEmpty()) {
                                            nySkjerming(skjermetHendelse);
                                            log.info("Ny skjerming opprettet fra aapen-nom-ressurs-v1 for ident: {}", navident);
                                        }
                                    }
                                }
                        );
        }

        private void nySkjerming(SkjermetPerson nySkjermetPerson){
            nySkjermetPerson.setSkjermetFra(LocalDateTime.now());
            nySkjermetPerson.setSkjermetTil(nySkjermetPerson.getSkjermetTil() != null ? nySkjermetPerson.getSkjermetTil().plusMonths(mnderSkjermetPlus) : nySkjermetPerson.getSkjermetTil());
            nySkjermetPerson.setEndretDato(LocalDateTime.now());
            nySkjermetPerson.setOpprettetDato(LocalDateTime.now());
            nySkjermetPerson.setOpprettetAv(Kilde.SERVICEBRUKER);
            nySkjermetPerson.setEndretAv(Kilde.SERVICEBRUKER);
            nySkjermetPerson.setKafkaProduced(false);
            repository.save(nySkjermetPerson);
        }
}
