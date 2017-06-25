package fabzo.kraken;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.text.StrSubstitutor;

public class EnvironmentContext {
    public static final String PORT_REF_TO = "%s.ports.%s.to";
    public static final String PORT_REF_FROM = "%s.ports.%s.from";
    public static final String IP_REF = "%s.ip";

    private final String salt;
    private Option<String> publicFacingIP = Option.none();
    private Map<String, String> environmentVariables = HashMap.empty();
    private List<Runnable> shutdownHooks = List.empty();

    public EnvironmentContext(final String salt) {
        this.salt = salt;
    }

    public void putEnv(final String name, final String value) {
        this.environmentVariables = environmentVariables.put(name, value);
    }

    public void addPort(final String componentName, final String portName, final int fromPort, final int toPort) {
        putEnv(String.format(PORT_REF_FROM, componentName, portName), String.valueOf(fromPort));
        putEnv(String.format(PORT_REF_TO, componentName, portName), String.valueOf(toPort));
    }

    public void setIP(final String componentName, final String ip) {
        putEnv(String.format(IP_REF, componentName), String.valueOf(ip));
    }

    public String salt() {
        return salt;
    }

    public Option<String> publicFacingIP() {
        return publicFacingIP;
    }

    public String resolve(final String text) {
        return StrSubstitutor.replace(text, environmentVariables.toJavaMap());
    }

    public void setPublicFacingIP(final String ip) {
        publicFacingIP = Option.of(ip);
    }

    public Option<Integer> port(final String name, final String portName) {
        return environmentVariables
                .get(String.format(PORT_REF_FROM, name, portName))
                .map(this::toInt);
    }

    private Integer toInt(final String port) {
        return Try.of(() -> Integer.valueOf(port)).getOrNull();
    }

    /**
     * Returns the IP assigned to the service by its lifecycle handler. If no IP has been
     * assigned this function will fallback to the public facing IP or localhost.
     *
     * @param name Service name
     * @return Service IP, public facing host IP, or localhost
     */
    public String ip(final String name) {
        return environmentVariables
                .get(String.format(IP_REF, name))
                .getOrElse(() -> publicFacingIP().getOrElse("localhost"));
    }

    public void registerShutdownHook(final Runnable shutdownHook) {
        shutdownHooks = shutdownHooks.append(shutdownHook);
    }

    public List<Runnable> shutdownHooks() {
        return shutdownHooks;
    }
}
