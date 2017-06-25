package fabzo.kraken.handler.docker.callbacks;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogContainerResultCallback extends ResultCallbackTemplate<LogContainerResultCallback, Frame> {
    private final String logPrefix;

    public LogContainerResultCallback(final String logPrefix) {
        this.logPrefix = logPrefix;
    }

    @Override
    public void onNext(final Frame item) {
        log.info("[{}] {}", logPrefix, item.toString());
    }
}