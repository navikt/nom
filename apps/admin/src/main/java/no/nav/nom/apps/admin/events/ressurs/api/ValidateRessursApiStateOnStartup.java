package no.nav.nom.apps.admin.events.ressurs.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ValidateRessursApiStateOnStartup {

    private final RessursApiStateV4Repository ressursApiStateV4Repository;
    private final RessursApiStateV3Repository ressursApiStateV3Repository;
    private final RessursApiStateV4Listener ressursApiStateV4Listener;
    private final RessursApiStateV3Listener ressursApiStateV3Listener;

    public ValidateRessursApiStateOnStartup(
        RessursApiStateV4Repository ressursApiStateV4Repository,
        RessursApiStateV3Repository ressursApiStateV3Repository,
        RessursApiStateV4Listener ressursApiStateV4Listener,
        RessursApiStateV3Listener ressursApiStateV3Listener
    ) {
        this.ressursApiStateV4Repository = ressursApiStateV4Repository;
        this.ressursApiStateV3Repository = ressursApiStateV3Repository;
        this.ressursApiStateV4Listener = ressursApiStateV4Listener;
        this.ressursApiStateV3Listener = ressursApiStateV3Listener;
    }

    @Async
    public void validate() {
        while (!ressursApiStateV4Listener.hasReadToEndOnStartup() || !ressursApiStateV3Listener.hasReadToEndOnStartup()) {
            try {
                Thread.sleep(5000); // Wait for 1 second before checking again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                throw new RuntimeException("Thread was interrupted while waiting for Kafka listeners to finish reading", e);
            }
        }

        try {
            log.info("ValidateRessursApiStateOnStartup: Starting");

            if (ressursApiStateV3Repository.getLatestRessursStatePerFid().size() != ressursApiStateV4Repository.getLatestRessursStatePerFid()
                .size()) {
                log.warn(
                    "ValidateRessursApiStateOnStartup: Mismatch in number of distinct navident entries between V3 and V4: V3={}, V4={}",
                    ressursApiStateV3Repository.getLatestRessursStatePerFid().size(),
                    ressursApiStateV4Repository.getLatestRessursStatePerFid().size()
                );
            }

            var counter = new AtomicInteger(0);
            ressursApiStateV3Repository.getLatestRessursStatePerFid().forEach((fid, ressursV3) -> {
                if (counter.incrementAndGet() % 1000 == 0) {
                    log.info(
                        "ValidateRessursApiStateOnStartup: Validating navident {} of {}",
                        counter.get(), ressursApiStateV3Repository.getLatestRessursStatePerFid().size()
                    );
                }
                if (!ressursApiStateV4Repository.getLatestRessursStatePerFid().containsKey(fid)) {
                    log.warn("ValidateRessursApiStateOnStartup: Navident {} found in V3 but not in V4", fid);
                    return;
                }

                var ressursV4 = ressursApiStateV4Repository.getLatestRessursStatePerFid().get(fid);
                if (ressursV4 == null || ressursV3 == null) {
                    log.warn("ValidateRessursApiStateOnStartup: One of the states is null for navident {}", fid);
                    return;
                }

                if (!Objects.equals(ressursV3.personident(), ressursV4.personident())) {
                    log.warn(
                        "ValidateRessursApiStateOnStartup: Personident mismatch for navident {}: V3={}, V4={}",
                        fid, ressursV3.personident().substring(0, 6) + "****", ressursV4.personident().substring(0, 6) + "****"
                    );

                }
                if (!Objects.equals(ressursV3.startdato(), ressursV4.startdato())) {
                    log.warn(
                        "ValidateRessursApiStateOnStartup: Startdato mismatch for navident {}: V3={}, V4={}",
                        fid, ressursV3.startdato(), ressursV4.startdato()
                    );
                }
                if (!Objects.equals(ressursV3.sluttdato(), ressursV4.sluttdato())) {
                    log.warn(
                        "ValidateRessursApiStateOnStartup: Sluttdato mismatch for navident {}: V3={}, V4={}",
                        fid, ressursV3.sluttdato(), ressursV4.sluttdato()
                    );
                }
                if (!Objects.equals(ressursV3.sektor(), ressursV4.sektor())) {
                    log.warn(
                        "ValidateRessursApiStateOnStartup: Sektor mismatch for navident {}: V3={}, V4={}",
                        fid, ressursV3.sektor(), ressursV4.sektor()
                    );
                }
                if (ressursV3.orgtilknytninger() == null && ressursV4.orgtilknytninger() != null) {
                    log.warn(
                        "ValidateRessursApiStateOnStartup: orgtilknytninger is null in V3 but not in V4 for navident {}",
                        fid
                    );
                } else if (ressursV3.orgtilknytninger() != null && ressursV4.orgtilknytninger() == null) {
                    log.warn(
                        "ValidateRessursApiStateOnStartup: orgtilknytninger is not null in V3 but null in V4 for navident {}",
                        fid
                    );
                } else if (ressursV3.orgtilknytninger() != null && ressursV3.orgtilknytninger().size() != ressursV4.orgtilknytninger().size()) {
                    log.warn(
                        "ValidateRessursApiStateOnStartup: Mismatch in number of orgtilknytninger for navident {}: V3={}, V4={}",
                        fid,
                        ressursV3.orgtilknytninger().size(),
                        ressursV4.orgtilknytninger().size()
                    );
                }
            });
            log.info("ValidateRessursApiStateOnStartup: Done - Thread {}", Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("ValidateRessursApiStateOnStartup: Error during validation", e);
        }
    }
}

