package fabzo.kraken;

import lombok.extern.slf4j.Slf4j;

import static fabzo.kraken.utils.Utils.nameOf;

@Slf4j
public class Kraken {

    public static Environment createEnvironment(final EnvironmentModule module) {
        log.info("Configuring module {}", nameOf(module));
        module.configure();

        return new Environment(module);
    }
}
