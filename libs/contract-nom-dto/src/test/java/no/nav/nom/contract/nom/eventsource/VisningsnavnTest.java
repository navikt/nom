package no.nav.nom.contract.nom.eventsource;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VisningsnavnTest {

    @Test
    void getFormattedVisningsnavn() {
        var visningsnavn = new Visningsnavn("Fornavn", Optional.empty(), "Etternavn");
        assertEquals("Fornavn Etternavn", visningsnavn.getFormattedVisningsnavn());

        visningsnavn = new Visningsnavn("Fornavn", Optional.of(""), "Etternavn");
        assertEquals("Fornavn Etternavn", visningsnavn.getFormattedVisningsnavn());

        visningsnavn = new Visningsnavn("Fornavn", Optional.of(" "), "Etternavn");
        assertEquals("Fornavn Etternavn", visningsnavn.getFormattedVisningsnavn());

        visningsnavn = new Visningsnavn("Fornavn", Optional.of("Mellomnavn"), "Etternavn");
        assertEquals("Fornavn Mellomnavn Etternavn", visningsnavn.getFormattedVisningsnavn());
    }

    @Test
    void getFornavnMellomnavn() {
        var visningsnavn = new Visningsnavn("Fornavn", Optional.empty(), "Etternavn");
        assertEquals("Fornavn", visningsnavn.getFornavnMellomnavn());

        visningsnavn = new Visningsnavn("Fornavn", Optional.of(""), "Etternavn");
        assertEquals("Fornavn", visningsnavn.getFornavnMellomnavn());

        visningsnavn = new Visningsnavn("Fornavn", Optional.of(" "), "Etternavn");
        assertEquals("Fornavn", visningsnavn.getFornavnMellomnavn());

        visningsnavn = new Visningsnavn("Fornavn", Optional.of("Mellomnavn"), "Etternavn");
        assertEquals("Fornavn Mellomnavn", visningsnavn.getFornavnMellomnavn());
    }
}