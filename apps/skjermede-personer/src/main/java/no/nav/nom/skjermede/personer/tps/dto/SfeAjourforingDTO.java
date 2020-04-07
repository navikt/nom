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
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SfeAjourforingDTO {

    @XmlElement
    private SystemInfoDTO systemInfo;

    @XmlElement
    private EndreEgenAnsattDTO endreEgenAnsatt;

    @XmlElement
    private OpphorEgenAnsattDTO opphorEgenAnsatt;
}
