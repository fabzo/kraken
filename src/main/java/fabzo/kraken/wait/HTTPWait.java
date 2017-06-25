package fabzo.kraken.wait;

import com.google.common.base.MoreObjects;
import fabzo.kraken.EnvironmentContext;
import fabzo.kraken.components.InfrastructureComponent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

@Slf4j
public class HTTPWait implements Wait {
    private final String url;
    private final String containsText;
    private final Duration atMost;

    public HTTPWait(final String url, final String containsText, final Duration atMost) {
        this.url = url;
        this.containsText = containsText;
        this.atMost = atMost;
    }

    @Override
    public boolean execute(final EnvironmentContext ctx, final InfrastructureComponent component) {
        val defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(2000)
                .setSocketTimeout(2000)
                .setConnectionRequestTimeout(2000)
                .build();

        val httpClient = HttpClients.custom()
                .setConnectionManager(new BasicHttpClientConnectionManager())
                .setDefaultRequestConfig(defaultRequestConfig)
                .build();

        await().atMost(atMost.toMillis(), TimeUnit.MILLISECONDS).until(() -> {
            try {
                val response = httpClient.execute(new HttpGet(url));
                val result = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                return containsText != null && result.contains(containsText);
            } catch(final Exception ignored) {
                return false;
            }
        });

        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("url", url)
                .add("containsText", containsText)
                .add("atMost", atMost)
                .toString();
    }
}
