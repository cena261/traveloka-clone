package com.cena.traveloka.common.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.cena.traveloka")
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String[] uris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.elasticsearch.socket-timeout:30s}")
    private Duration socketTimeout;

    @Value("${spring.elasticsearch.connection-timeout:10s}")
    private Duration connectionTimeout;

    @Value("${app.elasticsearch.max-connections:50}")
    private int maxConnections;

    @Value("${app.elasticsearch.max-connections-per-route:10}")
    private int maxConnectionsPerRoute;

    @Value("${app.elasticsearch.keep-alive-strategy:30s}")
    private Duration keepAliveStrategy;

    @Value("${app.elasticsearch.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${app.elasticsearch.ssl.verify-certificates:true}")
    private boolean verifyCertificates;

    @Value("${app.elasticsearch.sniffing.enabled:false}")
    private boolean sniffingEnabled;

    @Value("${app.elasticsearch.sniffing.interval:5m}")
    private Duration sniffingInterval;

    @Value("${app.elasticsearch.sniffing.delay-after-failure:1m}")
    private Duration sniffingDelayAfterFailure;

    @Bean
    public RestClient elasticsearchRestClient() {
        HttpHost[] hosts = Arrays.stream(uris)
            .map(uri -> HttpHost.create(uri))
            .toArray(HttpHost[]::new);

        RestClientBuilder builder = RestClient.builder(hosts);

        builder.setRequestConfigCallback(requestConfigBuilder ->
            requestConfigBuilder
                .setConnectTimeout((int) connectionTimeout.toMillis())
                .setSocketTimeout((int) socketTimeout.toMillis())
        );

        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder
                .setMaxConnTotal(maxConnections)
                .setMaxConnPerRoute(maxConnectionsPerRoute)
                .setKeepAliveStrategy((response, context) -> keepAliveStrategy.toMillis());

            if (username != null && !username.trim().isEmpty() &&
                password != null && !password.trim().isEmpty()) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }

            if (sslEnabled) {
                try {
                    if (!verifyCertificates) {
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null, new TrustManager[]{
                            new X509TrustManager() {
                                @Override
                                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                                @Override
                                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                                @Override
                                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                            }
                        }, null);
                        httpClientBuilder.setSSLContext(sslContext);
                        httpClientBuilder.setSSLHostnameVerifier((hostname, session) -> true);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to configure SSL for Elasticsearch", e);
                }
            }

            return httpClientBuilder;
        });

        return builder.build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}