package fabzo.kraken.docker;

import fabzo.kraken.AbstractDockerTest;
import fabzo.kraken.Environment;
import fabzo.kraken.EnvironmentModule;
import fabzo.kraken.Kraken;
import fabzo.kraken.components.DockerComponent;
import fabzo.kraken.handler.docker.DockerConfiguration;
import fabzo.kraken.handler.docker.DockerLifecycleHandler;
import org.junit.Test;

public class DockerFollowLogsTest extends AbstractDockerTest {

    @Test
    public void testFollowLogs() {
        final Environment environment = Kraken.createEnvironment(new EnvironmentModule() {
            @Override
            public void configure() {
                register(DockerLifecycleHandler.withConfig(
                        DockerConfiguration.create()
                                .withDockerSocket(DockerConfiguration.DOCKER_HOST_UNIX)));

                register(DockerComponent.create()
                        .withName("mariadb")
                        .withImage("alpine", "latest")
                        .withFollowLogs()
                        .withCommand("echo 'hello'"));

            }
        });

        try {
            environment.start();
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            environment.stop();
        }
    }
}
