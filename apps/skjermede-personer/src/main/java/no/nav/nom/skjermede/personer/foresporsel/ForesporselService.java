package no.nav.nom.skjermede.personer.foresporsel;

import com.microsoft.graph.models.extensions.Recipient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.skjermede.personer.foresporsel.mail.GraphService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForesporselService {

    private final ForesporselRepository foresporselRepository;
    private final GraphService graphService;

    @Value("${ressurser.api}")
    private String ressursAPI;

    @Value("${foresporsel.api}")
    private String foresporselAPI;

    public void sendForesporsler() {
        foresporselRepository.findAllByEpostSendtNull().forEach(foresporsel -> {
            final String content =
                    String.format("<h3>Godkjen forespørsel ved å trykke <a href=\"%s/godkjenn?id=%s\">her</a>",
                            foresporselAPI,
                            foresporsel.getFunksjonellId());

            try {
                graphService.sendMail(content, hentEpost(foresporsel.getTilknyttetRessurs()));
                foresporsel.setEpostSendt(LocalDate.now());
                foresporselRepository.save(foresporsel);
            } catch (Exception e) {
                log.error(String.format("Sending av skjerming forespørsel epost mislyktes: %s", e.getMessage()));
            }
        });
    }

    private String hentEpost(final String navident) {
        return new RestTemplate().getForObject(
                String.format("%s/epost/{navident}", ressursAPI),
                String.class,
                navident);
    }
}
