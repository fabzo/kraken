package fabzo.kraken.handler.kubernetes;

import fabzo.kraken.handler.HandlerConfiguration;
import io.fabric8.kubernetes.client.ConfigBuilder;

public class KubernetesConfiguration implements HandlerConfiguration {
    private boolean runDockerComponents = false;
    private String namespace = "default";
    private ConfigBuilder kubernetesConfig;
    private ServiceType defaultServiceType = ServiceType.DEFAULT;
    private boolean masterIPOnNodePort = false;
    private String registryPrefix;

    public static KubernetesConfiguration create() {
        return new KubernetesConfiguration();
    }

    public boolean runDockerComponents() {
        return runDockerComponents;
    }

    /**
     * If enabled the kubernetes lifecycle handler will also run docker components.
     * <br>
     * Note: Configure the kubernetes handler before the docker handler for it to have a higher priority.
     * <br>
     * Default: false
     */
    public KubernetesConfiguration withRunDockerComponents(final boolean runDockerComponents) {
        this.runDockerComponents = runDockerComponents;
        return this;
    }

    public String namespace() {
        return namespace != null ? namespace : "default";
    }

    /**
     * Sets the namespace in which pods and services are started.
     * <br>
     * Default: default
     */
    public KubernetesConfiguration withNamespace(final String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ConfigBuilder kubernetesConfig() {
        return kubernetesConfig;
    }

    /**
     * Kubernetes master to use.
     * <br>
     * Default: localhost
     */
    public KubernetesConfiguration withKubernetesConfig(final ConfigBuilder kubernetesConfig) {
        this.kubernetesConfig = kubernetesConfig;
        return this;
    }

    public ServiceType defaultServiceType() {
        return defaultServiceType;
    }

    /**
     * The default type for a newly created service.
     * <br>
     * Default: DEFAULT (type field not present)
     */
    public KubernetesConfiguration withDefaultServiceType(final ServiceType defaultServiceType) {
        this.defaultServiceType = defaultServiceType;
        return this;
    }

    public String registryPrefix() {
        return registryPrefix;
    }

    /**
     * The default project in which images for docker components can be found.
     * <br>
     * Default: none
     */
    public KubernetesConfiguration withRegistryPrefix(final String registryPrefix) {
        this.registryPrefix = registryPrefix;
        return this;
    }

    public boolean masterIPOnNodePort() {
        return masterIPOnNodePort;
    }

    /**
     * This will instruct the kubernetes lifecycle handler to use the master
     * instead of the cluster IP for the component. Mainly for the use with minikube.
     * <br>
     * Default: false
     */
    public KubernetesConfiguration withMasterIPOnNodePort(final boolean masterIPOnNodePort) {
        this.masterIPOnNodePort = masterIPOnNodePort;
        return this;
    }
}
