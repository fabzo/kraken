package fabzo.kraken.wait;

import java.time.Duration;

public class MySQLWait extends DatabaseWait {

    public MySQLWait(final String database, final String username, final String password, final String portName, final Duration atMost) {
        super("mysql", database, username, password, portName, atMost);
    }

    public MySQLWait(final String database, final String portName, final Duration atMost) {
        super("mysql", database, "root", "", portName, atMost);
    }
}
