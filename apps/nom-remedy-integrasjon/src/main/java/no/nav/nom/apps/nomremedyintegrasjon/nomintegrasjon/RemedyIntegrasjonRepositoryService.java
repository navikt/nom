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
    static final String UNLEASH_SKRU_AV_OVERFORING_TIL_REMEDY = "nom.remedy-integrasjon.skru_av_overforing_til_remedy";

    private final Logger logger = LoggerFactory.getLogger(RemedyIntegrasjonRepositoryService.class);
    private final Unleash unleash;

    private final NomIntegrasjonRepository nomIntegrasjonRepository;

    public RemedyIntegrasjonRepositoryService(Unleash unleash, NomIntegrasjonRepository nomIntegrasjonRepository) {
        this.unleash = unleash;
        this.nomIntegrasjonRepository = nomIntegrasjonRepository;
    }

    protected NomIntegrasjonEntity saveNewNomIntegrasjonEntityNew(NomIntegrasjonEntity newNomIntegrasjonEntity, String initiatedByNavident, boolean isAdmin) {
        validateThatRequiredValuesAreSet(newNomIntegrasjonEntity);

        if (unleash.isEnabled(UNLEASH_SKRU_AV_OVERFORING_TIL_REMEDY)) {
            logger.info("Overfører ikke endring til Remedy for {}", newNomIntegrasjonEntity.getNavId());
            return null;
        }

        if (isAdmin || nomIntegrasjonEntityProvidesNewDataFromNom(newNomIntegrasjonEntity)) {
            try {
                NomIntegrasjonEntity nomIntegrasjonEntity = nomIntegrasjonRepository.save(
                    newNomIntegrasjonEntity.toBuilder()
                        .entryId(nextId())
                        .entryOperasjon("Ny")
                        .entryStatus("Ubehandlet")
                        .entryBestiller(initiatedByNavident)
                        .build()
                );
                logger.info(
                    "Lagret ny rad for ressurs {}, til enhet {} og med entryId {}",
                    newNomIntegrasjonEntity.getNavId(),
                    newNomIntegrasjonEntity.getEnhetsId(),
                    nomIntegrasjonEntity.getEntryId()
                );
                return nomIntegrasjonEntity;
            } catch (Exception e) {
                logger.error(
                    "Feil ved lagring av ny rad for ressurs {}, til enhet {} og med entryId {}",
                    newNomIntegrasjonEntity.getNavId(),
                    newNomIntegrasjonEntity.getEnhetsId(),
                    newNomIntegrasjonEntity.getEntryId(),
                    e
                );
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

        // Skal ikke trenge å validere dette her - utløpsdato kan være null for eksterne med tilknytningstype Helfo og Skatt.
        // Kravet til utløpsdato for andre er satt og gjort i nomIntegrasjonEntityFromNomData(...) før denne kalles
//        if (nomIntegrasjonEntity.getSektor() == 2 && nomIntegrasjonEntity.getUtlopsdato() == null) {
//            throw RequiredValueMissingException.forField("utløpsdato");
//        }
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
}
