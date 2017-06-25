package fabzo.kraken;

import fabzo.kraken.components.InfrastructureComponent;
import fabzo.kraken.handler.LifecycleHandler;
import io.vavr.collection.List;

import static fabzo.kraken.EnvironmentContext.IP_REF;
import static fabzo.kraken.EnvironmentContext.PORT_REF_FROM;

/**
 * Abstract environment module used to setup new test environments.
 * Provides methods for registering infrastructure components and runners
 * as well as setting configuration values.
 */
public abstract class EnvironmentModule {
    private List<InfrastructureComponent> components = List.empty();
    private List<LifecycleHandler> handlers = List.empty();

    public abstract void configure();

    protected void register(final InfrastructureComponent component) {
        this.components = components.append(component);
    }

    protected void register(final LifecycleHandler runner) {
        this.handlers = handlers.append(runner);
    }

    List<InfrastructureComponent> components() {
        return components;
    }

    List<LifecycleHandler> handlers() {
        return handlers;
    }

    /**
     * Builds a port reference that can be used to reference the port
     * in component parameters.
     *
     * @param componentName Components name
     * @param portName Ports name
     * @return Port reference like ${:componentName:.ports.:portName:.from}
     */
    protected String portRef(final String componentName, final String portName) {
        return String.format("${" + PORT_REF_FROM + "}", componentName, portName);
    }

    /**
     * Builds a ip reference that can be used to reference the IP
     * in component parameters.
     *
     * @param componentName Components name
     * @return IP reference like ${:componentName:.ip}
     */
    protected String ipRef(final String componentName) {
        return String.format("${" + IP_REF + "}", componentName);
    }
}