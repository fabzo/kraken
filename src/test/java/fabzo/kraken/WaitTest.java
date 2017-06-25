package fabzo.kraken;

import fabzo.kraken.components.DockerComponent;
import fabzo.kraken.handler.docker.DockerConfiguration;
import fabzo.kraken.handler.docker.DockerLifecycleHandler;
import fabzo.kraken.wait.MySQLWait;
import org.junit.Test;

import java.time.Duration;

public class WaitTest {

    @Test
    public void testDatabaseWait() {
        final Environment environment = Kraken.createEnvironment(new EnvironmentModule() {
            @Override
            public void configure() {
                register(DockerLifecycleHandler.withConfig(
                        DockerConfiguration.create()
                                .withDockerSocket(DockerConfiguration.DOCKER_HOST_UNIX)));

                register(DockerComponent.create()
                        .withName("mariadb")
                        .withImage("mariadb", "latest")
                        .withForcePull()
                        .withFollowLogs()
                        .withPortBinding("db", 3306)
                        .withEnv("MYSQL_DATABASE", "testdb")
                        .withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes")
                        .withWait(new MySQLWait("testdb","db", Duration.ofSeconds(60))));

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
