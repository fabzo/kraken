package fabzo.kraken.handler.docker.callbacks;

import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.PullResponseItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;

public class NoLogPullImageResultCallback extends NoLogResultCallbackTemplate<NoLogPullImageResultCallback, PullResponseItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoLogPullImageResultCallback.class);

    @CheckForNull
    private PullResponseItem latestItem = null;

    @Override
    public void onNext(PullResponseItem item) {
        this.latestItem = item;
        LOGGER.debug(item.toString());
    }

    /**
     * Awaits the image to be pulled successful.
     *
     * @throws DockerClientException
     *             if the pull fails.
     */
    public void awaitSuccess() {
        try {
            awaitCompletion();
        } catch (InterruptedException e) {
            throw new DockerClientException("", e);
        }

        if (latestItem == null) {
            throw new DockerClientException("Could not pull image");
        } else if (!latestItem.isPullSuccessIndicated()) {
            String message = (latestItem.getError() != null) ? latestItem.getError() : latestItem.getStatus();
            throw new DockerClientException("Could not pull image: " + message);
        }
    }
}
