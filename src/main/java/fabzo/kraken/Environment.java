package fabzo.kraken;

import fabzo.kraken.components.InfrastructureComponent;
import fabzo.kraken.handler.LifecycleHandler;
import fabzo.kraken.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import static fabzo.kraken.utils.Utils.nameOf;

@Slf4j
public class Environment {
    private final EnvironmentModule module;
    private final EnvironmentContext context;

    public Environment(final EnvironmentModule module) {
        this.module = module;

        val salt = RandomStringUtils.randomAlphabetic(8).toLowerCase();
        log.info("Using environment salt {}", salt);
        this.context = new EnvironmentContext(salt);

        setupEnvironment();
    }

    private void setupEnvironment() {
        Utils.getPublicFacingIP().forEach(context::setPublicFacingIP);
        registerShutdownHook();
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> context.shutdownHooks().forEach(Runnable::run)));
    }

    public void start() {
        log.info("Starting all components of {}", nameOf(module));
        module.components().forEach(this::start);
    }

    private void start(final InfrastructureComponent component) {
        val handler = module.handlers().find(h -> h.canRun(component.getClass()));
        if (handler.isEmpty()) {
            throw new IllegalStateException("Could not find handler for " + component.name());
        }
        log.info("Starting {} using {}", component.name(), readableHandlerName(handler.get()));
        handler.get().run(component, context);
    }

    private String readableHandlerName(final LifecycleHandler handler) {
        val words = StringUtils.splitByCharacterTypeCamelCase(handler.getClass().getSimpleName());
        return String.join(" ", words).toLowerCase();
    }

    private void stop(final InfrastructureComponent component) {
        val handler = module.handlers().find(h -> h.canRun(component.getClass()));
        if (handler.isEmpty()) {
            throw new IllegalStateException("Could not find handler for " + component.name());
        }
        handler.get().stop(component, context);
    }

    public void stop() {
        log.info("Stopping all components of {}", nameOf(module));
        module.components().forEach(this::stop);
    }

    public EnvironmentContext context() {
        return context;
    }
}