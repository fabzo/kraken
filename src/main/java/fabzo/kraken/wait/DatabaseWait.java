package fabzo.kraken.wait;

import com.google.common.base.MoreObjects;
import fabzo.kraken.EnvironmentContext;
import fabzo.kraken.components.InfrastructureComponent;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.sql.DriverManager;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jayway.awaitility.Awaitility.await;

@Slf4j
public class DatabaseWait implements Wait {
    private static final String DEFAULT_JDBC_URL = "jdbc:%s://%s:%s/%s?user=%s&password=%s&useUnicode=true&characterEncoding=utf8&useSSL=false&nullNamePatternMatchesAll=true&socketTimeout=5000";
    private static final String TEST_SQL = "SELECT 1";

    private String username = "root";
    private String password = "";

    private String driver;
    private String database;
    private String portName;
    private final Duration atMost;

    private Option<String> connectionUrl = Option.none();

    public DatabaseWait(final String driver, final String database, final String username, final String password, final String portName, final Duration atMost) {
        this.driver = driver;
        this.database = database;
        this.portName = portName;
        this.username = username;
        this.password = password;
        this.atMost = atMost;
    }

    public DatabaseWait(final String connectionUrl, final Duration atMost) {
        this.connectionUrl = Option.of(connectionUrl);
        this.atMost = atMost;
    }

    private String createConnectionUrl(final InfrastructureComponent component, final EnvironmentContext ctx) {
        val ip = ctx.ip(component.name());
        val port = ctx.port(component.name(), portName);
        if (port.isEmpty()) {
            throw new IllegalStateException("Unable to retrieve port " + portName + " of component " + component.name());
        }

        return String.format(DEFAULT_JDBC_URL, driver, ip, port.get(), database, username, password);
    }

    @Override
    public boolean execute(final EnvironmentContext ctx, final InfrastructureComponent component) {
        val url = ctx.resolve(connectionUrl.getOrElse(() -> createConnectionUrl(component, ctx)));

        log.info("Waiting for database to become available for up to {}", atMost);
        log.info("Connection URL is {}", url);

        val properties = new Properties();
        properties.put("connectTimeout", "5000");

        val counter = new AtomicInteger();
        await().atMost(atMost.toMillis(), TimeUnit.MILLISECONDS).until(() -> {
            log.debug("Connection attempt ({}) to {}", counter.incrementAndGet(), database);
            try (val connection = DriverManager.getConnection(url, properties);
                 val statement = connection.prepareStatement(TEST_SQL)) {

                statement.execute();
                return true;
            } catch(final Exception ignored) {
                log.debug(" -> Attempt ({}) failed due to {}[{}]",
                        counter.get(),
                        ignored.getClass().getSimpleName(),
                        ignored.getMessage());
                return false;
            }
        });

        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("username", username)
                .add("driver", driver)
                .add("database", database)
                .add("portName", portName)
                .add("atMost", atMost)
                .add("connectionUrl", connectionUrl)
                .toString();
    }
}
