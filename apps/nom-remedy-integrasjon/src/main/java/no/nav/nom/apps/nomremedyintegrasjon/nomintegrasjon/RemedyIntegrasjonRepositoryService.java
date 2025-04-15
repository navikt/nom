package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import io.getunleash.Unleash;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.RequiredValueMissingException;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonEntity;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.fifteenCharLengthZeroPaddedNumber;

@Service
public class RemedyIntegrasjonRepositoryService {

    private final Logger logger = LoggerFactory.getLogger(RemedyIntegrasjonRepositoryService.class);

    private final NomIntegrasjonRepository nomIntegrasjonRepository;

    private final Unleash unleash;

    public RemedyIntegrasjonRepositoryService(NomIntegrasjonRepository nomIntegrasjonRepository,
                                              Unleash unleash) {
        this.nomIntegrasjonRepository = nomIntegrasjonRepository;
        this.unleash = unleash;
    }

    protected void saveNewNomIntegrasjonEntity(NomIntegrasjonEntity newNomIntegrasjonEntity, String initiatedByNavident) {
        if (!unleash.isEnabled("nom-remedy.ny-remedy-integrasjon")) {
            validateThatRequiredValuesAreSet(newNomIntegrasjonEntity);
            if (nomIntegrasjonEntityProvidesNewDataFromNom(newNomIntegrasjonEntity)) {
                nomIntegrasjonRepository.save(
                    newNomIntegrasjonEntity.toBuilder()
                        .entryId(nextId())
                        .entryOperasjon("Ny")
                        .entryStatus("Ubehandlet")
                        .entryBestiller(initiatedByNavident)
                        .build()
                );
            }
        }
    }

    protected NomIntegrasjonEntity saveNewNomIntegrasjonEntityNew(NomIntegrasjonEntity newNomIntegrasjonEntity, String initiatedByNavident) {
        if (unleash.isEnabled("nom-remedy.ny-remedy-integrasjon")) {
            validateThatRequiredValuesAreSet(newNomIntegrasjonEntity);

            if (nomIntegrasjonEntityProvidesNewDataFromNom(newNomIntegrasjonEntity)) {
                NomIntegrasjonEntity nomIntegrasjonEntity = nomIntegrasjonRepository.save(
                    newNomIntegrasjonEntity.toBuilder()
                        .entryId(nextId())
                        .entryOperasjon("Ny")
                        .entryStatus("Ubehandlet")
                        .entryBestiller(initiatedByNavident)
                        .build()
                );
                logger.info("Lagret ny rad for ressurs {}, til enhet {} og med entryId {}", newNomIntegrasjonEntity.getNavId(), newNomIntegrasjonEntity.getEnhetsId(), nomIntegrasjonEntity.getEntryId());
                return nomIntegrasjonEntity;
            } else {
                logger.info("Lagret ny rad for ressurs {}, til enhet {}", newNomIntegrasjonEntity.getNavId(), newNomIntegrasjonEntity.getEnhetsId());
            }
        }
        return null;
    }

    protected void markRessursForDeletion(String navident, String initiatedBy) {
        nomIntegrasjonRepository.save(
            NomIntegrasjonEntity.builder()
                .entryId(nextId())
                .navId(navident)
                .entryOperasjon("Slette")
                .entryStatus("Ubehandlet")
                .entryBestiller(initiatedBy)
                .build()
        );
    }

    private void validateThatRequiredValuesAreSet(NomIntegrasjonEntity nomIntegrasjonEntity) {
        if (nomIntegrasjonEntity.getEnhetsId() == null) {
            throw RequiredValueMissingException.forField("enhetsId");
        }
        if (nomIntegrasjonEntity.getEtternavn() == null) {
            throw RequiredValueMissingException.forField("etternavn");
        }
        if (nomIntegrasjonEntity.getFodselsnummer() == null) {
            throw RequiredValueMissingException.forField("fodselsnummer");
        }
        if (nomIntegrasjonEntity.getFornavn() == null) {
            throw RequiredValueMissingException.forField("fornavn");
        }
        if (nomIntegrasjonEntity.getFysiskBrukerstedId() == null) {
            throw RequiredValueMissingException.forField("fysiskBrukerstedId");
        }
        if (nomIntegrasjonEntity.getNavId() == null) {
            throw RequiredValueMissingException.forField("navId");
        }
        if (nomIntegrasjonEntity.getSektor() == null) {
            throw RequiredValueMissingException.forField("sektor");
        }
        if (nomIntegrasjonEntity.getTiltredelsesdato() == null) {
            throw RequiredValueMissingException.forField("tiltredelsesdato");
        }

        if (nomIntegrasjonEntity.getSektor() == 2 && nomIntegrasjonEntity.getUtlopsdato() == null) {
            throw RequiredValueMissingException.forField("utløpsdato");
        }
    }

    private boolean nomIntegrasjonEntityProvidesNewDataFromNom(NomIntegrasjonEntity newNomIntegrasjonEntity) {
        return nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(newNomIntegrasjonEntity.getNavId())
            .map(latestEntityForNavident -> !(
                Objects.equals(latestEntityForNavident.getEnhetsId(), newNomIntegrasjonEntity.getEnhetsId())
                    && Objects.equals(latestEntityForNavident.getEtternavn(), newNomIntegrasjonEntity.getEtternavn())
                    && Objects.equals(latestEntityForNavident.getFodselsnummer(), newNomIntegrasjonEntity.getFodselsnummer())
                    && Objects.equals(latestEntityForNavident.getFornavn(), newNomIntegrasjonEntity.getFornavn())
                    && Objects.equals(latestEntityForNavident.getFysiskBrukerstedId(), newNomIntegrasjonEntity.getFysiskBrukerstedId())
                    && Objects.equals(latestEntityForNavident.getNavId(), newNomIntegrasjonEntity.getNavId())
                    && Objects.equals(latestEntityForNavident.getSektor(), newNomIntegrasjonEntity.getSektor())
                    && Objects.equals(latestEntityForNavident.getTiltredelsesdato(), newNomIntegrasjonEntity.getTiltredelsesdato())
                    && Objects.equals(latestEntityForNavident.getUtlopsdato(), newNomIntegrasjonEntity.getUtlopsdato())
                    && Objects.equals(latestEntityForNavident.getMobil(), newNomIntegrasjonEntity.getMobil())
                    && Objects.equals(latestEntityForNavident.getMasseflytting(), newNomIntegrasjonEntity.getMasseflytting())
                    && Objects.equals(latestEntityForNavident.getDirekteTelefon(), newNomIntegrasjonEntity.getDirekteTelefon())
            ))
            .orElse(true);
    }

    // https://nav-it.slack.com/archives/C040RV7KCH5/p1696498426739059
    protected String nextId() {
        return nomIntegrasjonRepository.findTopByOrderByEntryIdDesc()
            .map(latestEntry -> Long.parseLong(latestEntry.getEntryId()))
            .map(latestId -> fifteenCharLengthZeroPaddedNumber(latestId + 1))
            .orElse(fifteenCharLengthZeroPaddedNumber(0L));
    }

    protected Optional<NomIntegrasjonEntity> findTopByNavIdEqualsOrderByEntryIdDesc(String navId) {
        return nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(navId);
    }

    protected List<NomIntegrasjonEntity> findAll() {
        return nomIntegrasjonRepository.findAll();
    }

    protected void deleteAll() {
        nomIntegrasjonRepository.deleteAll();
    }

    protected void save(NomIntegrasjonEntity nomIntegrasjonEntity) {
        nomIntegrasjonRepository.save(nomIntegrasjonEntity);
    }
}
