package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class RessursStillingLoadStatus {
    private final LeaderElection leaderElection;

    private Load currentLoad;

    public RessursStillingLoadStatus(LeaderElection leaderElection) {
        this.leaderElection = leaderElection;
    }

    public void startNewLoad() {
        currentLoad = new Load(LocalDateTime.now(), null, new ArrayList<>());
    }
    public void endLoad() {
        if (currentLoad == null) startNewLoad();
        currentLoad = currentLoad.end();
    }

    public void addError(String navident, String message) {
        if (currentLoad == null) startNewLoad();
        currentLoad.errors().add(new Error(navident, message));
    }
    public String report(String navident) {
        if (!leaderElection.isLeader()) return "Sry - traff pod som ikke er leader. Har da ikke status";

        if (currentLoad == null) return "Ingen kjøring tilgjenglig";
        if (currentLoad.ended == null) return "Kjøring ikke fullført";

        List<Error> errorsToReport =
            navident == null ?
                currentLoad.errors :
                currentLoad.errors.stream().filter( e -> e.navident.equals(navident)).toList();

        StringBuffer strBuf = new StringBuffer();
        strBuf.append(currentLoad.getLoadTiming());
        strBuf.append("\n");
        errorsToReport.forEach( e -> {
            strBuf.append("\n\n");
            strBuf.append(e.message);
        });
        return strBuf.toString();
    }

    private record Load(
        LocalDateTime started,
        LocalDateTime ended,
        List<Error> errors
    ) {
        Load end() {
            return new Load(this.started, LocalDateTime.now(), this.errors);
        }

        public String getLoadTiming() {
            return "Started - ended: %s - %s\nDuration %s".formatted(started, ended, durationAsH24(started, ended));
        }

        private String durationAsH24(LocalDateTime start, LocalDateTime end) {
            Duration duration = Duration.between(start, end);
            return  "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
        }
    }

    private record Error(
        String navident,
        String message
    ) {}
}
