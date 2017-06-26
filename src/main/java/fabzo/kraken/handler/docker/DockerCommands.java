package fabzo.kraken.handler.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import fabzo.kraken.handler.docker.callbacks.LogContainerResultCallback;
import fabzo.kraken.handler.docker.callbacks.NoLogPullImageResultCallback;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.val;

public class DockerCommands {
    private static final String API_VERSION = "1.23";
    private static final boolean VERIFY_TLS = false;

    private final String dockerSocket;
    private final DockerClient dockerClient;
    private List<DockerClient> logDockerClients = List.empty();

    public DockerCommands(final String dockerSocket, final String dockerRegistry) {
        this.dockerSocket = dockerSocket;
        this.dockerClient = newDockerClient(dockerSocket, dockerRegistry);

        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
    }

    private void cleanup() {
        Try.run(dockerClient::close);
        logDockerClients.forEach(client -> Try.run(client::close));
    }

    private DockerClient newDockerClient(final String dockerSocket, final String dockerRegistry) {
        val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerSocket)
                .withDockerTlsVerify(VERIFY_TLS)
                .withApiVersion(API_VERSION)
                .withRegistryUrl(dockerRegistry)
                .build();

        return DockerClientBuilder.getInstance(dockerConfig).build();
    }

    public void pullImage(final String image, final String tag) {
        dockerClient.pullImageCmd(image + ":" + tag)
                .exec(new NoLogPullImageResultCallback())
                .awaitSuccess();
    }

    public void stopContainer(final String id) {
        dockerClient.stopContainerCmd(id).exec();
    }

    public void removeContainer(final String id) {
        dockerClient.removeContainerCmd(id).exec();
    }

    public void startContainer(final String id) {
        dockerClient.startContainerCmd(id).exec();
    }

    public boolean isImageAvailable(final String image, final String tag) {
        try {
            dockerClient.inspectImageCmd(image + ":" + tag).exec();
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    public CreateContainerCmd createContainer(final String image) {
        return dockerClient.createContainerCmd(image);
    }

    public void logContainer(final String id, final String logPrefix) {
        val dockerClient = newDockerClient(dockerSocket, "");
        logDockerClients = logDockerClients.append(dockerClient);

        dockerClient.logContainerCmd(id)
                .withFollowStream(true)
                .withStdOut(true)
                .withStdErr(true)
                .exec(new LogContainerResultCallback(logPrefix));
    }
}
