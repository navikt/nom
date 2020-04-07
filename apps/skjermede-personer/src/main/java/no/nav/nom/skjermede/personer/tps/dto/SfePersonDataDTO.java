package no.nav.nom.skjermede.personer.tps.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "sfePersonData")
@XmlAccessorType(XmlAccessType.FIELD)
public class SfePersonDataDTO {

    @XmlElement(name = "sfeAjourforing")
    private SfeAjourforingDTO sfeAjourforing;

    @XmlElement(name = "sfeTilbakeMelding")
    private SfeTilbakeMeldingDTO sfeTilbakeMelding;

    public boolean isOk() {
        return sfeTilbakeMelding != null
               && sfeTilbakeMelding.getSvarStatus() != null
               && sfeTilbakeMelding.getSvarStatus().getReturStatus().equals("OO");
    }
}
