package fabzo.kraken.wait;

import com.google.common.base.MoreObjects;
import fabzo.kraken.EnvironmentContext;
import fabzo.kraken.components.InfrastructureComponent;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.temporal.TemporalUnit;

@Slf4j
public class TimeWait implements Wait {
    private final Duration duration;

    public TimeWait(final int amount, final TemporalUnit unit) {
        this(Duration.of(amount, unit));
    }

    public TimeWait(final Duration duration) {
        this.duration = duration;
    }

    @Override
    public boolean execute(final EnvironmentContext ctx, final InfrastructureComponent component) {
        log.info("Waiting {} for the service to become available", duration);
        try {
            Thread.sleep(duration.toMillis());
            return true;
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("duration", duration)
                .toString();
    }
}
