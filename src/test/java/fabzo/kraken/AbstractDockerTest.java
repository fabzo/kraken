package fabzo.kraken;

import org.junit.BeforeClass;

import java.io.File;

public abstract class AbstractDockerTest {
    private static final String DOCKER_SOCKET_FILE = "/var/run/docker.sock";
    private static final String DOCKER_SOCKET_NOT_FOUND = "Could not find '" + DOCKER_SOCKET_FILE + "' required for the docker integration tests.";

    @BeforeClass
    public static void beforeClass() {
        if (!new File(DOCKER_SOCKET_FILE).exists()) {
            throw new IllegalStateException(DOCKER_SOCKET_NOT_FOUND);
        }
    }
}