package no.nav.nom.apps.navident.pdl;

import no.nav.nom.apps.navident.SpringBootTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class PdlServiceImplTest extends SpringBootTestBase {
    @Autowired
    protected PdlService pdlService;

    @Test
    public void getIdenter_01019000001() {
        String ident = "01019000001";

        List<String> pldIdenter = pdlService.getKnownPersonider(ident); // kun 1 returnert, og den er gjeldende

        assertEquals(1, pldIdenter.size());
        assertEquals(ident, pldIdenter.get(0));
    }

    @Test
    public void getIdenter_01019000002() {
        String ident = "01019000002";
        String gjeldende = "01019200002";

        List<String> pldIdenter = pdlService.getKnownPersonider(ident); // 4 returnert, hvorav en aktorid, og 01019200002 er gjeldende

        assertEquals(3, pldIdenter.size());
        assertEquals(gjeldende, pldIdenter.get(0));
        assertTrue(pldIdenter.contains(ident));
    }

    @Test
    public void getIdenter_01019000003() {
        String ident = "01019000003";

        List<String> pldIdenter = pdlService.getKnownPersonider(ident); // 4 returnert, hvorav en aktorid, og 01019000003 er gjeldende

        assertEquals(3, pldIdenter.size());
        assertEquals(ident, pldIdenter.get(0));
    }

    @Test
    public void pldTimeout_LogError_Return_empty(CapturedOutput output) {
        String ident = "01019000004";

        List<String> pldIdenter = pdlService.getKnownPersonider(ident); // 4 returnert, hvorav en aktorid, og 01019000003 er gjeldende
        assertEquals(0, pldIdenter.size());
        assertTrue(output.getOut().contains("Read timed out"));
    }
}