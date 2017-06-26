package fabzo.kraken.handler.docker;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import fabzo.kraken.EnvironmentContext;
import fabzo.kraken.components.ComponentState;
import fabzo.kraken.components.DockerComponent;
import fabzo.kraken.components.InfrastructureComponent;
import fabzo.kraken.handler.AbstractLifecycleHandler;
import fabzo.kraken.utils.Utils;
import io.vavr.collection.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Lifecycle handler for docker.
 */
@Slf4j
public class DockerLifecycleHandler extends AbstractLifecycleHandler {
    private final DockerConfiguration config;
    private DockerCommands dockerCommands;
    private List<String> managedContainers = List.empty();

    protected DockerLifecycleHandler(final DockerConfiguration config) {
        this.config = config;
    }

    public static DockerLifecycleHandler withConfig(final DockerConfiguration config) {
        return new DockerLifecycleHandler(config);
    }

    @Override
    public void stop(final InfrastructureComponent component, final EnvironmentContext context) {
        val dockerC = (DockerComponent) component;

        if (managedContainers.contains(dockerC.id())) {
            log.info("[stopping] {}", component.name());

            dockerCommands.stopContainer(dockerC.id());
            component.setState(ComponentState.STOPPED);

            dockerCommands.removeContainer(dockerC.id());
            component.setState(ComponentState.DESTROYED);

            managedContainers = managedContainers.remove(dockerC.id());
        }
    }

    @Override
    protected boolean doRun(final InfrastructureComponent component, final EnvironmentContext ctx) {
        throwIfNotSupported(component);
        val dockerC = (DockerComponent) component;

        if (isRunning(dockerC)) {
            return true;
        }

        if (isStopped(dockerC)) {
            // Stopped but not destroyed. Cannot do anything right now
            return false;
        }

        ensureDockerCommands();

        dockerC.setState(ComponentState.CREATING);
        pullImageIfRequired(dockerC);
        createContainer(dockerC, ctx);

        log.info("Starting {}", dockerC.name());
        dockerC.setState(ComponentState.STARTING);
        dockerCommands.startContainer(dockerC.id());
        dockerC.setState(ComponentState.STARTED);

        if (dockerC.isFollowLogs()) {
            dockerCommands.logContainer(dockerC.id(), dockerC.name());
        }

        managedContainers = managedContainers.append(dockerC.id());
        ctx.registerShutdownHook(() -> stop(dockerC, ctx));

        return true;
    }

    private void createContainer(final DockerComponent dockerC, final EnvironmentContext ctx) {
        log.debug("Creating {}", dockerC.name());
        final CreateContainerResponse createResult = dockerCommands
                .createContainer(dockerC.image() + ":" + dockerC.tag())
                .withName(String.format("%s_%s", ctx.salt(), dockerC.name()))
                .withPortBindings(buildPortBindings(dockerC, ctx, dockerC))
                .withEnv(buildEnvironment(ctx, dockerC))
                .exec();

        dockerC.setId(createResult.getId());
    }

    private void pullImageIfRequired(final DockerComponent dockerC) {
        val image = dockerC.image();
        val tag = dockerC.tag();

        if (dockerC.isForcePull() || dockerCommands.isImageAvailable(image, tag)) {
            log.info("Pulling image {}:{}", image, tag);
            try {
                dockerCommands.pullImage(image, tag);
            } catch (final NotFoundException e) {
                log.warn("Cannot pull image: {}", e.getMessage());
            }
        }
    }

    private void ensureDockerCommands() {
        if (dockerCommands == null) {
            dockerCommands = new DockerCommands(config.dockerSocket(), config.dockerRegistry());
        }
    }

    private java.util.List<String> buildEnvironment(final EnvironmentContext ctx, final DockerComponent dockerC) {
        return dockerC.env().foldRight(List.<String>empty(), (nameValue, list) -> {
            return list.append(String.format("%s=%s", nameValue._1, ctx.resolve(nameValue._2)));
        }).toJavaList();
    }

    private java.util.List<PortBinding> buildPortBindings(final InfrastructureComponent component, final EnvironmentContext ctx, final DockerComponent dockerC) {
        return dockerC.ports().foldRight(List.<PortBinding>empty(), (namePort, list) -> {
            final Integer highPort = Utils.getAvailablePort().get();
            ctx.addPort(component.name(), namePort._1, highPort, namePort._2);
            return list.append(createPortBinding(highPort, namePort._2));
        }).toJavaList();
    }

    private PortBinding createPortBinding(final int hostPort, final int containerPort) {
        final Ports.Binding binding = new Ports.Binding("0.0.0.0", String.valueOf(hostPort));
        final ExposedPort exposedPort = new ExposedPort(containerPort);
        return new PortBinding(binding, exposedPort);
    }

    @Override
    public boolean canRun(final Class<? extends InfrastructureComponent> clazz) {
        return DockerComponent.class.isAssignableFrom(clazz);
    }

    private void throwIfNotSupported(final InfrastructureComponent component) {
        if (!(component instanceof DockerComponent)) {
            throw new IllegalArgumentException(String.format(
                    "%s was given a infrastructure component of type %s (%s) which it cannot execute",
                    getClass().getSimpleName(),
                    component.getClass().getSimpleName(),
                    component.name()
            ));
        }
    }
}
