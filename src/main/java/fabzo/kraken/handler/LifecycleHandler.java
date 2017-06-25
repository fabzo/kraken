package fabzo.kraken.handler;

import fabzo.kraken.EnvironmentContext;
import fabzo.kraken.components.InfrastructureComponent;

public interface LifecycleHandler {

    boolean canRun(final Class<? extends InfrastructureComponent> clazz);

    boolean run(final InfrastructureComponent component, final EnvironmentContext ctx);

    void stop(final InfrastructureComponent component, final EnvironmentContext ctx);
}
