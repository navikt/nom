package no.nav.nom.apps.nom.enheter.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.enheter.enhet.Enhet;
import no.nav.nom.apps.nom.enheter.enhet.EnhetRepository;
import no.nav.nom.apps.nom.enheter.enhet.Organisering;
import no.nav.nom.apps.nom.enheter.enhet.OrganiseringRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DvhConsumerProcessorService {

    private final EnhetRepository enhetRepository;
    private final OrganiseringRepository organiseringRepository;

    public void processResult(List<DvhEnhetDTO> dvhEnhetList){
        processEnheter(dvhEnhetList.stream().map(this::convertedEnhet).collect(Collectors.toList()));
        processOrganiseringer(dvhEnhetList.stream().map(this::convertedOrganisering).collect(Collectors.toList()));
    }

    private void processEnheter(List<Enhet> enhetList){
        enhetList.forEach(
                        enhet -> {
                            Enhet eksisterendeEnhet = enhetRepository.findByDvhIdAndGyldigTilIsNull(enhet.getDvhId());
                            if(!Objects.equals(enhet, eksisterendeEnhet)) {
                                if (eksisterendeEnhet != null) {
                                    eksisterendeEnhet.setGyldigTil(enhet.getGyldigFra());
                                    eksisterendeEnhet.setEndretAv(enhet.getOpprettetAv());
                                    eksisterendeEnhet.setEndretDato(enhet.getOpprettetDato());
                                    enhetRepository.save(eksisterendeEnhet);

                                    enhet.setEnhetsnr(eksisterendeEnhet.getEnhetsnr());
                                } else {
                                    enhet.setEnhetsnr(enhetRepository.getNextEnhetNr());
                                }
                                enhetRepository.save(enhet);
                            }
                        });
    }

    private void processOrganiseringer(List<Organisering> organiseringList){
        organiseringList.stream()
                .filter(organisering -> {
                    if(organisering.getOverenhet() == null){
                        log.error("Forkastet organisering fra underenhet: "+organisering.getUnderenhet()+", overEnhet mangler i databasen.");
                        return false;
                    }
                    return true;
                })
                .forEach(organisering -> {
                    Organisering eksisterende = organiseringRepository.findByOverenhetAndUnderenhetAndOrganiseringstypeAndGyldigTilIsNull(organisering.getOverenhet(), organisering.getUnderenhet(), organisering.getOrganiseringstype());
                    if(!Objects.equals(organisering, eksisterende)){
                        if(eksisterende != null){
                            eksisterende.setGyldigTil(organisering.getGyldigFra());
                            eksisterende.setEndretAv(organisering.getOpprettetAv());
                            eksisterende.setEndretDato(eksisterende.getOpprettDato());
                            organiseringRepository.save(eksisterende);
                        }
                        organiseringRepository.save(organisering);
                    }
                });
    }

    private Enhet convertedEnhet(DvhEnhetDTO enhet){
        LocalDateTime currentTime = LocalDateTime.now();
        return Enhet.builder()
                .dvhId(enhet.getEnhetKode()+enhet.getNodeNivaa())
                .navn(enhet.getEnhetNavn())
                .enhetstype(enhet.getEnhetType())
                .agressoNr(enhet.getAgressoNr())
                .remedyNr(enhet.getRemedyNr())
                .gyldigFra(enhet.getEnhetGyldigFra() != null ? enhet.getEnhetGyldigFra() : currentTime.toLocalDate())
                .gyldigTil(enhet.getEnhetGyldigTil())
                .opprettetAv("DVH")
                .opprettetDato(currentTime)
                .endretAv("DVH")
                .endretDato(currentTime)
                .build();
    }

    private Organisering convertedOrganisering(DvhEnhetDTO enhet){
        LocalDateTime currentTime = LocalDateTime.now();
        return Organisering.builder()
                .overenhet(enhetRepository.findByDvhId(enhet.getOverEnhetKode()+enhet.getNodeNivaaOver()).stream().findAny().map(Enhet::getEnhetsnr).orElse(null))
                .underenhet(enhetRepository.findByDvhId(enhet.getEnhetKode()+enhet.getNodeNivaa()).stream().findAny().map(Enhet::getEnhetsnr).orElse(null))
                .organiseringstype(enhet.getOrganiseringsType())
                .gyldigFra(enhet.getOrgGyldigFra() != null ? enhet.getOrgGyldigFra() : currentTime.toLocalDate())
                .gyldigTil(enhet.getOrgGyldigTil())
                .opprettetAv("DVH")
                .opprettDato(currentTime)
                .endretAv("DVH")
                .endretDato(currentTime)
                .build();
    }

}
