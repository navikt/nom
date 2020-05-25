package no.nav.nom.skjermede.personer.tps;

import com.ibm.mq.jms.MQQueue;
import com.ibm.msg.client.wmq.compat.jms.internal.JMSC;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.nom.skjermede.personer.tps.config.JmsConfigProperties;
import no.nav.nom.skjermede.personer.tps.dto.EndreEgenAnsattDTO;
import no.nav.nom.skjermede.personer.tps.dto.OpphorEgenAnsattDTO;
import no.nav.nom.skjermede.personer.tps.dto.SfeAjourforingDTO;
import no.nav.nom.skjermede.personer.tps.dto.SfePersonDataDTO;
import no.nav.nom.skjermede.personer.tps.dto.SystemInfoDTO;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TpsService {

    private static final String KILDE_SYSTEM = "NOM";
    private static final String BRUKERIDENT = "srvnomskjermet";
    private static final String ENDRING = "endringsmelding";
    private static final String OPPHOR = "opphorsmelding";

    private final JmsTemplate jmsTemplate;
    private final MessageConverter messageConverter;
    private final ConnectionFactory connectionFactory;
    private final JmsConfigProperties jmsConfigProperties;
    private final SkjermetPersonRepository skjermetPersonRepository;
    private final MeterRegistry meterRegistry;


    @Transactional(value = "transactionManager")
    void sendAndUpdate(final SkjermetPerson skjermetPerson) {
        try (val connection = connectionFactory.createConnection()) {
            connection.start();

            val destinationQueue = new MQQueue(jmsConfigProperties.getQueueName());
            destinationQueue.setTargetClient(JMSC.MQJMS_CLIENT_NONJMS_MQ);

            if (skjermetPerson.getEndringSendtTps() == null) {
                sendMelding(skjermetPerson, connection, destinationQueue, true);
                meterRegistry.counter("tps_messages_sent_count", "type", "endring").increment();
            }

            if (skjermetPerson.getSkjermetTil() != null && skjermetPerson.getOpphorSendtTps() == null) {
                sendMelding(skjermetPerson, connection, destinationQueue, false);
                meterRegistry.counter("tps_messages_sent_count", "type", "opphor").increment();
            }
        } catch (Exception e) {
            log.error("Feil intraff ved synkronisering av skjerming informasjon mot tsp: {}", e.getMessage());
        }
    }

    private void sendMelding(final SkjermetPerson skjermetPerson,
                             final Connection connection,
                             final MQQueue destination,
                             final boolean isEndring) {
        try (val session = connection.createSession(); val producer = session.createProducer(destination)) {
            val replyQueue = session.createTemporaryQueue();
            val melding = messageConverter.toMessage(buildPersondata(skjermetPerson, isEndring), session);

            melding.setJMSReplyTo(replyQueue);
            producer.send(melding);
            validateReply(mottaSvarmelding(session, replyQueue));

            if (isEndring) {
                skjermetPerson.setEndringSendtTps(LocalDateTime.now());
            } else {
                skjermetPerson.setOpphorSendtTps(LocalDateTime.now());
            }

            skjermetPersonRepository.save(skjermetPerson);
        } catch (Exception e) {
            throw new TpsMqException(String.format("Sending av %s til tps mislyktes", (isEndring ? ENDRING : OPPHOR)));
        }
    }

    private void validateReply(final SfePersonDataDTO reply) {
        if (!reply.isOk()) {
            val feilmelding = reply.getSfeTilbakeMelding().getSvarStatus().getReturnMelding();
            throw new TpsMqException(feilmelding);
        }
    }

    private SfePersonDataDTO mottaSvarmelding(final Session session, final TemporaryQueue replyQueue) {
        try (val consumer = session.createConsumer(replyQueue)) {
            val message = consumer.receive(2000L);
            return (SfePersonDataDTO) messageConverter.fromMessage(message);
        } catch (Exception e) {
            throw new TpsMqException(String.format("Mottak av svarmelding fra tps mislyktes: {} : {}", e.getMessage(), e.getCause().getMessage()));
        }
    }

    private SfePersonDataDTO buildPersondata(final SkjermetPerson skjermetPerson, final boolean isEndring) {
        val systemInfo = SystemInfoDTO.builder().kildeSystem(KILDE_SYSTEM).brukerident(BRUKERIDENT).build();
        val endring = EndreEgenAnsattDTO.builder()
                .personident(skjermetPerson.getPersonident())
                .skjermetFom(skjermetPerson.getSkjermetFra().toLocalDate())
                .build();
        val opphor =  new OpphorEgenAnsattDTO(skjermetPerson.getPersonident());
        val sfeAjourforing = isEndring
                ? SfeAjourforingDTO.builder().systemInfo(systemInfo).endreEgenAnsatt(endring).build()
                : SfeAjourforingDTO.builder().systemInfo(systemInfo).opphorEgenAnsatt(opphor).build();

        return SfePersonDataDTO.builder().sfeAjourforing(sfeAjourforing).build();
    }
}
