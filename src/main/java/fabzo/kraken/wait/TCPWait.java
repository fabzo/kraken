package fabzo.kraken.wait;

import com.google.common.base.MoreObjects;
import com.jayway.awaitility.Duration;
import fabzo.kraken.EnvironmentContext;
import fabzo.kraken.components.InfrastructureComponent;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import static com.jayway.awaitility.Awaitility.await;

@Slf4j
public class TCPWait implements Wait {
    private final String portName;
    private final Duration atMost;

    public static TCPWait on(final String portName, final Duration atMost) {
        return new TCPWait(portName, atMost);
    }

    private TCPWait(final String portName, final Duration atMost) {
        this.portName = portName;
        this.atMost = atMost;
    }

    @Override
    public boolean execute(final EnvironmentContext ctx, final InfrastructureComponent component) {
        final Option<Integer> port = ctx.port(component.name(), portName);

        if (port.isEmpty()) {
            log.warn("Unable to get port {} of {}", portName, component.name());
            return false;
        }

        log.debug("Waiting for TCP port {} being available", port.get());
        await().atMost(atMost).until(() -> {
            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ctx.ip(component.name()), port.get()), 2000);
                socket.getOutputStream();
                return true;
            } catch (final IOException ignored) {
                return false;
            } finally {
                if (socket != null) {
                    Try.run(socket::close);
                }
            }
        });

        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("portName", portName)
                .add("atMost", atMost)
                .toString();
    }
}
