package fabzo.kraken;

import fabzo.kraken.handler.docker.DockerConfiguration;
import fabzo.kraken.handler.docker.DockerLifecycleHandler;
import fabzo.kraken.handler.kubernetes.KubernetesConfiguration;
import fabzo.kraken.handler.kubernetes.KubernetesLifecycleHandler;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

@Slf4j
public abstract class AbstractKubernetesTest {
    private static final String MINIKUBE_VERSION_PREFIX = "minikube version: ";
    private static final String KUBECTL_CONFIGURED_MESSAGE = "Kubectl is now configured to use the cluster.";

    @BeforeClass
    public static void beforeClass() {
        val minikubeVersion = minikubeVersion();
        log.info("Found minikube version {}", minikubeVersion);

        val minikubeStatus = minikubeStatus();

        if (minikubeStatus.length() == 0) {
            log.info("Minikube not running, starting...");
            val result = miniKubeStart();
            if (!result._1) {
                throw new IllegalStateException("Failed to start minikube. Output: " + result._2);
            }
        }

        val ip = minikubeIP();
        log.info("Minikube running at {}", ip);
    }

    private static String minikubeIP() {
        return runMinicubeCommand(List.of("ip")).trim();
    }

    protected KubernetesLifecycleHandler kubernetesLifecycleHandler() {
        return KubernetesLifecycleHandler.withConfig(
                KubernetesConfiguration.create()
                    .withRunDockerComponents(true)
                    .withMaster("https://" + minikubeIP() + ":8443"));
    }

    public static void afterClass() {
        // stop minicube if it has previously been stopped
    }

    private static Tuple2<Boolean, String> miniKubeStart() {
        val output = runMinicubeCommand(List.of("start"));
        log.info("START OUTPUT: {}", output);
        return new Tuple2<Boolean, String>(output.contains(KUBECTL_CONFIGURED_MESSAGE), output);
    }

    private static String minikubeVersion() {
        val output = runMinicubeCommand(List.of("version"));
        return output.substring(MINIKUBE_VERSION_PREFIX.length());
    }

    private static String minikubeStatus() {
        val output = runMinicubeCommand(List.of("status"));

        val parts = output.split("\n");
        if (parts.length < 1) {
            throw new IllegalStateException("Failed to extract status line from minikube output. Status returned: " + output);
        }

        val status = parts[0].substring("minikube:".length());
        return status.trim();
    }

    private static String runMinicubeCommand(List<String> args) {
        val cmd = "minikube " + args.mkString(" ");
        val builder = new ProcessBuilder("/bin/sh", "-c", cmd);

        try {
            val process = builder.start();
            val output = IOUtils.toString(new InputStreamReader(process.getInputStream()));
            process.waitFor();

            return output.trim();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Missing minikube. Tried to execute minikube with " + args.mkString(" ") + " but failed.");
        }
    }
}