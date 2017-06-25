package fabzo.kraken.handler.docker;

import fabzo.kraken.handler.HandlerConfiguration;

public class DockerConfiguration implements HandlerConfiguration {
    private static final String DOCKER_UNIX_SOCKET_FILE = "/var/run/docker.sock";
    public static final String DOCKER_HOST_UNIX = "unix://" + DOCKER_UNIX_SOCKET_FILE;
    public static final String DOCKER_HOST_TCP = "tcp://localhost:2376";

    private String dockerSocket = DOCKER_HOST_UNIX;
    private String dockerRegistry = "https://registry.hub.docker.com/v1";

    public static DockerConfiguration create() {
        return new DockerConfiguration();
    }

    public DockerConfiguration withDockerSocket(final String dockerSocket) {
        this.dockerSocket = dockerSocket;
        return this;
    }

    public String dockerSocket() {
        return dockerSocket;
    }

    public DockerConfiguration withDockerRegistry(final String dockerRegistry) {
        this.dockerRegistry = dockerRegistry;
        return this;
    }

    public String dockerRegistry() {
        return dockerRegistry;
    }
}
