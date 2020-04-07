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
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "systemInfo")
public class SystemInfoDTO {

    @XmlElement(name = "kilde")
    private String kildeSystem;

    @XmlElement(name = "brukerID")
    private String brukerident;
}
