package no.nav.nom.apps.nom.unit4.kafka;

public class KafkaRessursBlockingException extends RuntimeException {

    public KafkaRessursBlockingException(String message) {
        super(message);
    }

    public KafkaRessursBlockingException(Throwable throwable) {super(throwable);
    }
}
