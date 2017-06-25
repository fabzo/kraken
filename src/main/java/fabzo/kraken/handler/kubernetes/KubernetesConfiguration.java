package fabzo.kraken.handler.kubernetes;

import fabzo.kraken.handler.HandlerConfiguration;

public class KubernetesConfiguration implements HandlerConfiguration {
    private boolean runDockerComponents = false;
    private String namespace = "default";
    private String master;
    private boolean exposeService = false;
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

    public String master() {
        return master;
    }

    /**
     * Kubernetes master to use.
     * <br>
     * Default: localhost
     */
    public KubernetesConfiguration withMaster(final String master) {
        this.master = master;
        return this;
    }

    public boolean exposeService() {
        return exposeService;
    }

    /**
     * If services should be exposed (type: LoadBalancer).
     * <br>
     * Default: false
     */
    public KubernetesConfiguration withExposeService(final boolean exposeService) {
        this.exposeService = exposeService;
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
}
