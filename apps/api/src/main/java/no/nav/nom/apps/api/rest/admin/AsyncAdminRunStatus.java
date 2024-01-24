package no.nav.nom.apps.api.rest.admin;

import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AsyncAdminRunStatus {
    private final LeaderElection leaderElection;

    private Map<String, Run> currentRun = new HashMap<>();

    public AsyncAdminRunStatus(LeaderElection leaderElection) {
        this.leaderElection = leaderElection;
    }

    public String startNewRun(String runType) {
        if (currentRun.containsKey(runType)) {
            return "Kjøring allerede startet - %s".formatted(runType);
        }

        var run = new Run(LocalDateTime.now(), null, new ArrayList<>());
        currentRun.put(runType, run);
        return "Kjøring startet - %s".formatted(runType);
    }

    public void runEnded(String runType) {
        var run = currentRun.get(runType);
        if (run == null) startNewRun(runType);
        currentRun.put(runType, currentRun.get(runType).end());
    }

    public boolean isEnded(String runType) {
        var run = currentRun.get(runType);
        return run == null || run.ended != null;
    }

    public void addError(String runType, String navident, String message) {
        var run = currentRun.get(runType);
        if (run == null) startNewRun(runType);
        currentRun.get(runType).errors().add(new Error(navident, message));
    }
    public String report(String runType, String navident) {
        if (!leaderElection.isLeader()) return "Sry - traff pod som ikke er leader. Har da ikke status";

        var run = this.currentRun.get(runType);
        if (run == null) return "Ingen kjøring tilgjenglig";
        if (run.ended == null) return "Kjøring ikke fullført";

        List<Error> errorsToReport =
            navident == null ?
                run.errors :
                run.errors.stream().filter( e -> e.navident.equals(navident)).toList();

        StringBuffer strBuf = new StringBuffer();
        strBuf.append(run.getRunTiming(runType));
        strBuf.append("\n");
        errorsToReport.forEach( e -> {
            strBuf.append("\n\n");
            strBuf.append(e.message);
        });

        currentRun.remove(runType);

        return strBuf.toString();
    }

    private record Run(
        LocalDateTime started,
        LocalDateTime ended,
        List<Error> errors
    ) {
        Run end() {
            return new Run(this.started, LocalDateTime.now(), this.errors);
        }

        public String getRunTiming(String runType) {
            return "%s: Started - ended: %s - %s\nDuration %s".formatted(runType, started, ended, durationAsH24(started, ended));
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
