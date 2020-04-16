package no.nav.nom.skjermede.personer.tps.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.skjermede.personer.tps.XmlDateAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "endreEgenAnsatt")
public class EndreEgenAnsattDTO {

    @XmlElement(name = "offentligIdent")
    private String personident;

    @XmlElement(name = "fom")
    @XmlJavaTypeAdapter(XmlDateAdapter.class)
    private LocalDate skjermetFom;

    @XmlElement(name = "tom")
    @XmlJavaTypeAdapter(XmlDateAdapter.class)
    private LocalDate skjermetTom;
}
