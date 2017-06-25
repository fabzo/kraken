package fabzo.kraken.wait;

import fabzo.kraken.EnvironmentContext;
import fabzo.kraken.components.InfrastructureComponent;

public interface Wait {
    boolean execute(final EnvironmentContext ctx, final InfrastructureComponent component);
}
