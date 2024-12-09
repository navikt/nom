package no.nav.nom.paarorende.api;

import lombok.Builder;
import no.nav.nom.paarorende.jpa.Adresse;
import no.nav.nom.paarorende.jpa.Kontaktinfo;
import no.nav.nom.paarorende.jpa.Relasjon;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Builder
public record KontaktinfoDTO(

    String kontaktId,
    String fornavn,
    String etternavn,
    String relasjon,
    String telefon,
    String gateadresse,
    String postnr,
    String poststed,
    String land
) {

    public static KontaktinfoDTO fromEntity(Kontaktinfo kontaktinfo) {
        var adresse = Optional.ofNullable(kontaktinfo.getAdresse()).orElse(new Adresse());

        return KontaktinfoDTO.builder()
            .kontaktId(kontaktinfo.getKontakId())
            .fornavn(kontaktinfo.getFornavn())
            .etternavn(kontaktinfo.getEtternavn())
            .relasjon(kontaktinfo.getRelasjon().name())
            .telefon(kontaktinfo.getTelefon())
            .gateadresse(adresse.getAdresse())
            .postnr(adresse.getPostnr())
            .poststed(adresse.getPoststed())
            .land(adresse.getLand())
            .build();
    }

    public static List<KontaktinfoDTO> fromEntityList(List<Kontaktinfo> kontaktinfoList) {
        return kontaktinfoList
            .stream().map(KontaktinfoDTO::fromEntity)
            .toList();
    }

    public static Kontaktinfo toEntity(KontaktinfoDTO kontaktInfoDTO) {
        return Kontaktinfo.builder()
            .kontakId(UUID.randomUUID().toString().substring(0, 12))
            .fornavn(kontaktInfoDTO.fornavn)
            .etternavn(kontaktInfoDTO.etternavn)
            .relasjon(Relasjon.valueOf(kontaktInfoDTO.relasjon))
            .telefon(kontaktInfoDTO.telefon)
            .adresse(
                Adresse.builder()
                    .adresse(kontaktInfoDTO.gateadresse())
                    .postnr(kontaktInfoDTO.postnr())
                    .poststed(kontaktInfoDTO.poststed())
                    .land(kontaktInfoDTO.land())
                    .build())
            .build();
    }
}
