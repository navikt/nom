package no.nav.nom.skjermede.personer.tps;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;

public class XmlDateAdapter extends XmlAdapter<String, LocalDate> {

    @Override
    public LocalDate unmarshal(final String date) {
        return LocalDate.parse(date);
    }

    @Override
    public String marshal(final LocalDate date) {
        return date.toString();
    }
}
