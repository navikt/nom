package no.nav.nom.skjermede.personer.tps;

import com.ibm.mq.jms.MQQueue;
import com.ibm.msg.client.wmq.compat.jms.internal.JMSC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.nom.skjermede.personer.tps.dto.EndreEgenAnsattDTO;
import no.nav.nom.skjermede.personer.tps.dto.OpphorEgenAnsattDTO;
import no.nav.nom.skjermede.personer.tps.dto.SfeAjourforingDTO;
import no.nav.nom.skjermede.personer.tps.dto.SfePersonDataDTO;
import no.nav.nom.skjermede.personer.tps.dto.SystemInfoDTO;
import no.nav.nom.skjermede.personer.tps.exception.TpsMqException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TpsClientService {

    private static final String KILDE_SYSTEM = "NOM";
    private static final String BRUKERIDENT = "srvnom-skjermet";

    private final JmsTemplate jmsTemplate;
    private final MessageConverter messageConverter;
    private final ConnectionFactory connectionFactory;
    private final JmsConfigProperties jmsConfigProperties;
    private final SkjermetPersonRepository skjermetPersonRepository;

    void sendAndUpdate(final SkjermetPerson skjermetPerson) {
        try (val connection = connectionFactory.createConnection()) {
            connection.start();
            val session = connection.createSession();
            val replyQueue = session.createTemporaryQueue();

            send(skjermetPerson, session, new MQQueue(jmsConfigProperties.getQueueName()), replyQueue);
            handleReply(mottaSvarmelding(session, replyQueue));
            log.info("Melding sendt til TPS");
        } catch (Exception e) {
            log.error("Feil intraff ved synkronisering av skjerming informasjon mot tsp: {}", e.getCause().getMessage());
        }
    }

    @Transactional
    void handleReply(final SfePersonDataDTO reply) {
        if (!reply.isOk()) {
            String feilmelding = reply.getSfeTilbakeMelding() != null
                    ? reply.getSfeTilbakeMelding().getSvarStatus().getReturnMelding()
                    : Strings.EMPTY;
            throw new TpsMqException(String.format("Registring av skjerming info av tps mislyktes: %s", feilmelding));
        }

        val personident = reply.getSfeAjourforing().getEndreEgenAnsatt().getPersonident();
        val persistentSkjermetPerson = skjermetPersonRepository
                .getSkjermetPersonByPersonident(personident)
                .orElseThrow(() -> new RuntimeException("Ugyldig personident mottatt i svarmelding"));
        log.info(persistentSkjermetPerson.getFornavn());
        persistentSkjermetPerson.setSendtTilTps(LocalDate.now());
        skjermetPersonRepository.save(persistentSkjermetPerson);
    }

    private void send(final SkjermetPerson skjermetPerson, final Session session, final MQQueue destinationQueue, final TemporaryQueue replyQueue) {
        try (val producer = session.createProducer(destinationQueue)) {
            destinationQueue.setTargetClient(JMSC.MQJMS_CLIENT_NONJMS_MQ);

            val message = messageConverter.toMessage(buildPersondata(skjermetPerson), session);
            message.setJMSReplyTo(replyQueue);
            producer.send(message);
        } catch (Exception e) {
            throw new TpsMqException("Sending av melding til tps mislyktes");
        }
    }

    private SfePersonDataDTO mottaSvarmelding(final Session session, final TemporaryQueue replyQueue) {
        try (val consumer = session.createConsumer(replyQueue)) {
            val message = consumer.receive(2000L);
            log.info("Message: " + ((TextMessage) message).getText());
            val reply =  message.getBody(SfePersonDataDTO.class);
            log.info("Navident: " + reply.getSfeAjourforing().getSystemInfo().getBrukerident());
            return reply;
        } catch (Exception e) {
            throw new TpsMqException("Mottak av svarmelding fra tps mislyktes");
        }
    }

    private SfePersonDataDTO buildPersondata(final SkjermetPerson skjermetPerson) {
        val systemInfo = SystemInfoDTO.builder().kildeSystem(KILDE_SYSTEM).brukerident(BRUKERIDENT).build();
        val endring = EndreEgenAnsattDTO.builder().personident(skjermetPerson.getPersonident()).skjermetFom(LocalDate.now()).build();
        val opphor =  new OpphorEgenAnsattDTO(skjermetPerson.getPersonident());
        val sfeAjourforing = skjermetPerson.getSkjermetTom() == null
                ? SfeAjourforingDTO.builder().systemInfo(systemInfo).endreEgenAnsatt(endring).build()
                : SfeAjourforingDTO.builder().systemInfo(systemInfo).opphorEgenAnsatt(opphor).build();

        return SfePersonDataDTO.builder().sfeAjourforing(sfeAjourforing).build();
    }
}
