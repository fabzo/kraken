package fabzo.kraken.components;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

public class DockerComponent extends InfrastructureComponent {
    private String id;
    private String name;
    private String image;
    private String tag;
    private String command;
    private boolean forcePull = false;
    private boolean followLogs = false;
    private Map<String, Integer> ports = HashMap.empty();
    private Map<String, String> env = HashMap.empty();

    public static DockerComponent create() {
        return new DockerComponent();
    }

    protected DockerComponent() {

    }

    public DockerComponent withName(final String name) {
        this.name = name;
        return this;
    }

    public DockerComponent withImage(final String image, final String tag) {
        this.image = image;
        this.tag = tag;
        return this;
    }

    public DockerComponent withPortBinding(final String portName, final Integer port) {
        this.ports = this.ports.put(portName, port);
        return this;
    }

    public DockerComponent withEnv(final String name, final String value) {
        this.env = this.env.put(name, value);
        return this;
    }

    public DockerComponent withForcePull() {
        this.forcePull = true;
        return this;
    }

    public DockerComponent withFollowLogs() {
        this.followLogs = true;
        return this;
    }

    public DockerComponent withCommand(final String command) {
        this.command = command;
        return this;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String name() {
        return name;
    }

    public String image() {
        return image;
    }

    public String tag() {
        return tag;
    }

    public Map<String, Integer> ports() {
        return ports;
    }

    public Map<String, String> env() {
        return env;
    }

    public String id() {
        return id;
    }

    public boolean isForcePull() {
        return forcePull;
    }

    public boolean isFollowLogs() {
        return followLogs;
    }

    public String command() {
        return command;
    }
}
