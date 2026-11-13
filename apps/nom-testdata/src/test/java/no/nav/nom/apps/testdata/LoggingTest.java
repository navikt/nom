package no.nav.nom.apps.testdata;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingTest {

    private static final Marker TEAM_LOGS = MarkerFactory.getMarker("TEAM_LOGS");

    @Test
    void localStdoutRejectsTeamLogsAndAcceptsOrdinaryEvents() throws Exception {
        LoggerContext context = loadConfiguration("/logback.xml");
        try {
            Appender<ILoggingEvent> stdout = appender(context, "STDOUT");
            assertThat(decide(stdout, event(context, null))).isEqualTo(FilterReply.NEUTRAL);
            assertThat(decide(stdout, event(context, TEAM_LOGS))).isEqualTo(FilterReply.DENY);
        } finally {
            context.stop();
        }
    }

    @Test
    void naisStdoutRejectsTeamLogsAndTeamAppenderAcceptsOnlyMarkedEvents() throws Exception {
        LoggerContext context = loadConfiguration("/logback-nais.xml");
        try {
            Appender<ILoggingEvent> stdout = appender(context, "STDOUT_JSON");
            Appender<ILoggingEvent> teamLogs = appender(context, "TEAM_LOGS");

            assertThat(decide(stdout, event(context, null))).isEqualTo(FilterReply.NEUTRAL);
            assertThat(decide(stdout, event(context, TEAM_LOGS))).isEqualTo(FilterReply.DENY);
            assertThat(decide(teamLogs, event(context, null))).isEqualTo(FilterReply.DENY);
            assertThat(decide(teamLogs, event(context, TEAM_LOGS))).isEqualTo(FilterReply.ACCEPT);
        } finally {
            context.stop();
        }
    }

    private static LoggerContext loadConfiguration(String resource) throws Exception {
        LoggerContext context = new LoggerContext();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try (InputStream input = LoggingTest.class.getResourceAsStream(resource)) {
            assertThat(input).as("logging configuration %s exists", resource).isNotNull();
            configurator.doConfigure(input);
        }
        return context;
    }

    @SuppressWarnings("unchecked")
    private static Appender<ILoggingEvent> appender(LoggerContext context, String name) {
        return context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender(name);
    }

    private static FilterReply decide(Appender<ILoggingEvent> appender, ILoggingEvent event) {
        return appender.getCopyOfAttachedFiltersList().get(0).decide(event);
    }

    private static ILoggingEvent event(LoggerContext context, Marker marker) {
        Logger logger = context.getLogger(LoggingTest.class);
        LoggingEvent event = new LoggingEvent(LoggingTest.class.getName(), logger, Level.INFO,
            "synthetic neutral message", null, null);
        event.addMarker(marker);
        return event;
    }
}
