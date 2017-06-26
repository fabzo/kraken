# Kraken

Kraken allows you to create a test environment based on docker containers.

Use MySQL instead of H2, Kafka instead of embedded or mocked versions or spin up a Redis container.

## A few notes:
* This is a hobby project
* Test coverage could be better ... for now
* I'm using it to get a feel for project lombok and vavr
* Contribution are welcome


## Example usage
Creating a new test environment is done by implementing a new EnvironmentModule and registering LifecycleHandlers as well as InfrastructureComponents. The following example registers the DockerLifecycleHandler as well as a DockerComponent.
```java
public class MyTestingModule extends EnvironmentModule {
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
}
```

Using Kraken we can now create a new Environment from the previously defined module, start and stop it:
```java
final Environment environment = Kraken.createEnvironment(new MyTestingModule());

try {
	environment.start();
} catch (final Exception e) {
	e.printStackTrace();
} finally {
	environment.stop();
}
```
And Kraken will spin up a new mariadb:
```
12:14:16.424 [main] INFO  fabzo.kraken.Kraken - Configuring module fabzo.kraken.WaitTest$1
12:14:16.482 [main] INFO  fabzo.kraken.Environment - Using environment salt aggkkcfp
12:14:16.491 [main] INFO  fabzo.kraken.Environment - Starting all components of fabzo.kraken.WaitTest$1
12:14:16.495 [main] INFO  fabzo.kraken.Environment - Starting mariadb using docker handler
12:14:16.937 [main] INFO  f.k.handler.docker.DockerHandler - Pulling image mariadb:latest
12:14:19.959 [main] INFO  f.k.handler.docker.DockerHandler - Starting mariadb
12:14:20.562 [main] INFO  f.k.handler.AbstractLifecycleHandler - Waiting for mariadb using MySQLWait{username=root, driver=mysql, database=testdb, portName=db, atMost=PT1M, connectionUrl=None}
12:14:20.564 [main] INFO  fabzo.kraken.wait.DatabaseWait - Waiting for database to become available for up to PT1M
12:14:20.564 [main] INFO  fabzo.kraken.wait.DatabaseWait - Connection URL is jdbc:mysql://192.168.99.1:57294/testdb?user=root&password=&useUnicode=true&characterEncoding=utf8&useSSL=false&nullNamePatternMatchesAll=true
12:14:21.086 [dockerjava-jaxrs-async-1] INFO  f.k.h.d.c.LogContainerResultCallback - [mariadb] STDOUT: Initializing database
...
```

## Example for local and kubernetes usage
The following test module uses some environment variable to detect if it is running in kubernetes or on a simple host with docker. In case it runs in kubernetes it configures a KubernetesLifecycleHandler with the ability to run DockerComponents (it is plannend to have more sophisticated KubernetesComponents).
```java
public class MyTestingModule extends EnvironmentModule {
    public static final String MARIA_DB = "mariadb";

    public void configure() {
        val runningInKubernetes = System.getenv("SOME_ENV_VARIABLE") != null;

        if (runningInKubernetes) {
            register(KubernetesLifecycleHandler.withConfig(
                    KubernetesConfiguration.create()
                            .withRunDockerComponents(true)
                            .withExposeService(false)
                            .withNamespace("default")));
        } else {
            register(DockerLifecycleHandler.withConfig(
                    DockerConfiguration.create()
                            .withDockerSocket(DockerLifecycleHandler.DOCKER_HOST_UNIX)));
        }

        register(new DockerComponent()
                .withName(MARIA_DB)
                .withImage("mariadb", "latest")
                .withForcePull()
                .withPortBinding("db", 3306)
                .withEnv("MYSQL_DATABASE", "testdb")
                .withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes")
                .withWait(new MySQLWait("testdb","db", Duration.ofSeconds(60))));
    }
}
```
In order to ensure that your environment is always available when running integration tests you can create a super class that takes care of starting the environment. It will create the environment, start it and retrieve the mariadb ip and port to set as system propertries.
```java
public class AbstractIntegrationTests {
    private static final String MYSQL_PORT = "mysqlport";
    private static final String MYSQL_IP = "mysqlip";

    private static Environment environment;

    @BeforeClass
    public static void beforeClass() throws IOException, InterruptedException {
        if (environment == null) {
            environment = Kraken.createEnvironment(new MyTestingModule());
            environment.start();

            final EnvironmentContext ctx = environment.context();

            final Option<String> mariaDBPort = ctx
                    .port(MARIA_DB, "db")
                    .getOrElseThrow(() -> new IllegalStateException("Unable to retrieve maria db port"));
            System.setProperty(MYSQL_PORT, mariaDBPort.toString());

			// We are not getting an Option<String> here since the ip()
            // method automatically falls back to the public facing ip or
            // localhost.
            final String mariaDBIP = environment.context().ip(MARIA_DB);
            System.setProperty(MYSQL_IP, mariaDBIP);
        }
    }
}
```

## Property replacement
The Docker and Kubernetes lifecycle handlers use the EnvironmentContext to store
information like ports and IPs of started components. These can in turn be used
when configuring the environment variables on components.

Inside the EnvironmentModule:
* Port references can be created with portRef(component name, port name)
* IP references can be created with ipRef(component name)

Outside the environment on the EnvironmentContext
* Use the env(name) method to retrieve an environment variable set by one of the handlers
* Use port(component name, port name) to retrieve a port
* Use ip(component name) to retrieve the IP of a component
