package com.inplay.collector.kbo;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

public final class KboScheduleClient {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestClient restClient;
    private final URI scheduleUrl;

    public KboScheduleClient(RestClient restClient, URI scheduleUrl) {
        this.restClient = Objects.requireNonNull(restClient, "restClient required");
        this.scheduleUrl = Objects.requireNonNull(scheduleUrl, "scheduleUrl required");
    }

    public URI buildUrl(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from required");
        Objects.requireNonNull(to, "to required");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to must not be before from: " + from + " > " + to);
        }
        return UriComponentsBuilder.fromUri(scheduleUrl)
                .queryParam("from", DATE.format(from))
                .queryParam("to", DATE.format(to))
                .build(true)
                .toUri();
    }

    public String fetchScheduleHtml(LocalDate from, LocalDate to) {
        URI url = buildUrl(from, to);
        String body = restClient.get().uri(url).retrieve().body(String.class);
        return body == null ? "" : body;
    }

    public URI scheduleUrl() {
        return scheduleUrl;
    }
}
