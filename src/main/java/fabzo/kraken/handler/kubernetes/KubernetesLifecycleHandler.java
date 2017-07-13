package fabzo.kraken.handler.kubernetes;

import fabzo.kraken.EnvironmentContext;
import fabzo.kraken.components.ComponentState;
import fabzo.kraken.components.DockerComponent;
import fabzo.kraken.components.InfrastructureComponent;
import fabzo.kraken.components.KubernetesComponent;
import fabzo.kraken.handler.AbstractLifecycleHandler;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

@Slf4j
public class KubernetesLifecycleHandler extends AbstractLifecycleHandler {
    private KubernetesClient kubernetes;

    private Map<String, ReplicationController> managedReplicationControllers = HashMap.empty();
    private Map<String, Service> managedServices = HashMap.empty();

    private final KubernetesConfiguration config;

    public static KubernetesLifecycleHandler withConfig(final KubernetesConfiguration config) {
        return new KubernetesLifecycleHandler(config);
    }

    protected KubernetesLifecycleHandler(final KubernetesConfiguration config) {
        this.config = config;
    }

    @Override
    public void stop(final InfrastructureComponent component, final EnvironmentContext ctx) {
        if (component instanceof DockerComponent) {
            stopDockerComponent((DockerComponent)component, ctx);
        } else if (component instanceof KubernetesComponent) {
            stopKubernetesComponent((KubernetesComponent)component, ctx);
        }
    }

    private void stopKubernetesComponent(final KubernetesComponent component, final EnvironmentContext ctx) {
        throw new IllegalStateException("Kubernetes components are not yes supported.");
    }

    private void stopDockerComponent(final DockerComponent component, final EnvironmentContext ctx) {
        component.setState(ComponentState.STOPPED);
        log.info("[deleting] {} (pod)", component.name());
        managedReplicationControllers.get(component.name()).forEach(replicationController -> {
            kubernetes.replicationControllers()
                    .inNamespace(config.namespace())
                    .delete(replicationController);
        });

        log.info("[deleting] {} (service)", component.name());
        managedServices.get(component.name()).forEach(service -> {
            kubernetes.services()
                    .inNamespace(config.namespace())
                    .delete(service);
        });
        component.setState(ComponentState.DESTROYED);

        managedServices = managedServices.remove(component.name());
    }

    @Override
    public boolean doRun(final InfrastructureComponent component, final EnvironmentContext ctx) {
        throwIfNotSupported(component);

        if (component.state() != ComponentState.WAITING
                && component.state() != ComponentState.DESTROYED) {
            return true;
        }

        if (kubernetes == null) {
            if (config.master() == null) {
                kubernetes = new DefaultKubernetesClient();
            } else {
                kubernetes = new DefaultKubernetesClient(config.master());
            }
        }

        if (component instanceof DockerComponent) {
            return runDockerComponent((DockerComponent)component, ctx);
        } else if (component instanceof KubernetesComponent) {
            return runKubernetesComponent((KubernetesComponent)component, ctx);
        }

        return false;
    }

    private boolean runKubernetesComponent(final KubernetesComponent component, final EnvironmentContext ctx) {
        return false;
    }

    private boolean runDockerComponent(final DockerComponent component, final EnvironmentContext ctx) {

        // Use a default configuration for docker components consisting of a replication controller and service
        component.setState(ComponentState.CREATING);
        val replicationController = buildDockerReplicationController(component, ctx);
        managedReplicationControllers = managedReplicationControllers.put(component.name(), replicationController);

        val service = buildDockerService(component, ctx);
        managedServices = managedServices.put(component.name(), service);


        component.setState(ComponentState.STARTING);
        log.info("Creating {} (pod)", component.name());
        kubernetes.replicationControllers()
                .inNamespace(config.namespace())
                .create(replicationController);

        log.info("Creating {} (service)", component.name());
        kubernetes.services()
                .inNamespace(config.namespace())
                .create(service);

        // TODO: Check if really started?

        component.setState(ComponentState.STARTED);

        ctx.registerShutdownHook(() -> stop(component, ctx));

        // TODO: Which IP should be registered? Is the ClusterIP the correct one?
        registerWithContext(component, ctx);

        return false;
    }

    private ReplicationController buildDockerReplicationController(final DockerComponent component, final EnvironmentContext ctx) {
        val image = Option.of(config.registryPrefix())
                .map(prefix -> prefix + "/" + component.image() + ":" + component.tag())
                .getOrElse(component.image() + ":" + component.tag());

        //@formatter:off
        return new ReplicationControllerBuilder()
            .withNewMetadata()
                .withName(saltedName(ctx.salt(), component.name()))
                .addToLabels("salt", ctx.salt())
                .addToLabels("app", component.name())
                .addToLabels("scope", "integration-tests")
                .endMetadata()
            .withNewSpec()
                .withReplicas(1)
                .withNewTemplate()
                    .withNewMetadata()
                        .addToLabels("salt", ctx.salt())
                        .addToLabels("app", component.name())
                        .addToLabels("scope", "integration-tests")
                        .endMetadata()
                    .withNewSpec()
                        .addNewContainer()
                            .withImagePullPolicy(component.isForcePull() ? "Always" : "IfNotPresent")
                            .withName(component.name())
                            .withImage(image)
                            .addAllToPorts(createContainerPorts(component, ctx))
                            .withEnv(createEnvVars(component, ctx))
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();
        //@formatter:on
    }

    private Service buildDockerService(final DockerComponent component, final EnvironmentContext ctx) {
        //@formatter:off
        val servicePart = new ServiceBuilder()
            .withNewMetadata()
                .withName(saltedName(ctx.salt(), component.name()))
                .addToLabels("salt", ctx.salt())
                .addToLabels("app", component.name())
                .addToLabels("scope", "integration-tests")
                .endMetadata()
            .withNewSpec()
                .addAllToPorts(createServicePorts(component, ctx))
                .addToSelector("salt", ctx.salt())
                .addToSelector("app", component.name())
                .addToSelector("scope", "integration-tests");
        //@formatter:on

        final Service service;
        switch (config.defaultServiceType()) {
            case NODE_PORT:
                service = servicePart.withType("NodePort").endSpec().build();
                break;
            case LOAD_BALANCER:
                service = servicePart.withType("LoadBalancer").endSpec().build();
                break;
            case DEFAULT:
            default:
                service = servicePart.endSpec().build();
                break;
        }

        return service;
    }

    private void registerWithContext(final DockerComponent component, final EnvironmentContext ctx) {
        val services = kubernetes.services()
                .inNamespace(config.namespace())
                .withLabel("salt", ctx.salt())
                .withLabel("app", component.name())
                .withLabel("scope", "integration-tests")
                .list();

        if (services.getItems().size() <= 0) {
            throw new IllegalStateException("Failed to retrieve previously started service of " + component.name());
        }

        val serviceSpec = services.getItems().get(0).getSpec();

        ctx.setIP(component.name(), serviceSpec.getClusterIP());

        // TODO: Using default service type as there are no kubernetes components right now
        switch (config.defaultServiceType()) {
            case NODE_PORT:
                if (config.masterIPOnNodePort()) {
                    // Set the master IP for this component (useful with minikube)
                    ctx.setIP(component.name(), getHost(config.master()));
                } else {
                    ctx.setIP(component.name(), serviceSpec.getClusterIP());
                }
                // Find corresponding node ports for the components registered ports
                component.ports().forEach((name, port) -> {
                    serviceSpec.getPorts().forEach(servicePort -> {
                        if (servicePort.getPort().equals(port)) {
                            ctx.addPort(component.name(), name, servicePort.getNodePort(), port);
                        }
                    });
                });
                break;
            case LOAD_BALANCER:
                ctx.setIP(component.name(), serviceSpec.getLoadBalancerIP());
                component.ports().forEach((name, port) -> {
                    ctx.addPort(component.name(), name, port, port);
                });
                break;
            case DEFAULT:
            default:
                ctx.setIP(component.name(), serviceSpec.getClusterIP());
                component.ports().forEach((name, port) -> {
                    ctx.addPort(component.name(), name, port, port);
                });
                break;
        }

    }

    private String getHost(final String master) {
        try {
            return new URL(config.master()).getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            log.error("Failed to extract host from {}", master);
            return null; // TODO: Abort or ignore?
        }
    }

    private java.util.List<EnvVar> createEnvVars(final DockerComponent dockerC, final EnvironmentContext ctx) {
        return dockerC.env().foldRight(List.<EnvVar>empty(), (nameValue, list) -> {
            return list.append(new EnvVarBuilder()
                    .withName(nameValue._1)
                    .withValue(ctx.resolve(nameValue._2))
                    .build());
        }).toJavaList();
    }

    private String saltedName(final String salt, final String name) {
        return salt + "-" + name;
    }

    private Collection<ContainerPort> createContainerPorts(final DockerComponent dockerC, final EnvironmentContext ctx) {
        return dockerC.ports().foldRight(List.<ContainerPort>empty(), (namePort, list) -> {
            return list.append(newContainerPort(namePort._2));
        }).toJavaList();
    }

    private ContainerPort newContainerPort(final int port) {
        return new ContainerPortBuilder().withContainerPort(port).build();
    }

    private Collection<ServicePort> createServicePorts(final DockerComponent dockerC, final EnvironmentContext ctx) {
        return dockerC.ports().foldRight(List.<ServicePort>empty(), (namePort, list) -> {
            return list.append(newServicePort(namePort._2));
        }).toJavaList();
    }

    private ServicePort newServicePort(final int port) {
        return new ServicePortBuilder().withPort(port).withNewTargetPort(port).build();
    }

    private void followLog(final InfrastructureComponent component, final EnvironmentContext ctx) {
        val pods = kubernetes.pods()
                .inNamespace(config.namespace())
                .withLabel("salt", ctx.salt())
                .withLabel("app", component.name())
                .withLabel("scope", "integration-tests")
                .list();

        pods.getItems().forEach(pod -> {
            val name = pod.getMetadata().getName();
            kubernetes.pods()
                    .inNamespace(config.namespace())
                    .withName(name)
                    .tailingLines(10)
                    .watchLog(new OutpuStreamLogAdapter(log, component.name(), true));
        });
    }

    @Override
    public boolean canRun(final Class<? extends InfrastructureComponent> clazz) {
        if (DockerComponent.class.isAssignableFrom(clazz)) {
            return config.runDockerComponents();
        }

        return KubernetesComponent.class.isAssignableFrom(clazz);
    }

    private void throwIfNotSupported(final InfrastructureComponent component) {
        if (!(component instanceof DockerComponent || component instanceof KubernetesComponent)) {
            throw new IllegalArgumentException(String.format(
                    "KubernetesRunner was given a infrastructure component of type %s (%s) which is not runnable",
                    component.getClass().getSimpleName(),
                    component.name()
            ));
        }
    }
}
