package com.fillthepockets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReportImageEndpointSecurityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void reportImagesAreNotAvailableWithoutAuthentication() {
        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/reports/1/image", byte[].class);

        assertTrue(response.getStatusCode().is4xxClientError(),
                "Unauthenticated image requests must be denied.");
    }
}
