package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service("KafkaReadiness")
public class KafkaReadiness implements HealthIndicator {

    private final static long ACCEPTABLE_REBALANCING_TIME_MS = 10*1000;

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    // timestamp of earliest stream state transition into `state == rebalancing`, since most recent `state == running`
    // -1 if `state == running` was more recent than `state == rebalancing`
    private final AtomicLong rebalancingSinceTimeStamp;

    public KafkaReadiness(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
        this.rebalancingSinceTimeStamp = new AtomicLong(System.currentTimeMillis()-ACCEPTABLE_REBALANCING_TIME_MS); // assuming rebalancing is initial state;
    }
    @Override
    public @Nullable Health health(boolean includeDetails) {
        var streams = streamsBuilderFactoryBean.getKafkaStreams();
        if(streams == null) {
            return Health.down().build();
        }
        var streamsState = streams.state();
        return switch(streamsState) {
            case RUNNING -> {
                resetRebalancingCounter();
                yield Health.up().build();
            }
            case REBALANCING -> evaluateHealthWhenRebalancing(System.currentTimeMillis());
            case NOT_RUNNING, ERROR -> Health.outOfService().build();
            case PENDING_ERROR, PENDING_SHUTDOWN, CREATED -> Health.down().build();
        };
    }

    @Override
    public @Nullable Health health() {
        return this.health(false);
    }

    private Health evaluateHealthWhenRebalancing(long nowTimeMs){
        var refTimeMsTmp = rebalancingSinceTimeStamp.get();
        if(refTimeMsTmp == -1L){
            rebalancingSinceTimeStamp.set(nowTimeMs);
        }
        var refTimeMs = refTimeMsTmp == -1L ? nowTimeMs : refTimeMsTmp;
        var timeSinceLastRebalance = nowTimeMs - refTimeMs;
        var evaluateAsDown = timeSinceLastRebalance > ACCEPTABLE_REBALANCING_TIME_MS;
        return evaluateAsDown ? Health.down().build() : Health.up().build();
    }

    private void resetRebalancingCounter(){
        rebalancingSinceTimeStamp.set(-1);
    }

}
