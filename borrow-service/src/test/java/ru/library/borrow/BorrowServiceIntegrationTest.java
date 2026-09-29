package ru.library.borrow;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:borrow-test;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "server.error.include-message=always",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.client.simple.instances.book-service[0].uri=http://localhost:18081",
    "spring.cloud.discovery.client.simple.instances.user-service[0].uri=http://localhost:18082"
})
class BorrowServiceIntegrationTest {
    private static final WireMockServer BOOKS = new WireMockServer(options().port(18081));
    private static final WireMockServer USERS = new WireMockServer(options().port(18082));

    @Autowired
    private TestRestTemplate http;

    @DynamicPropertySource
    static void discoveryProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.book-service[0].uri", () -> "http://localhost:18081");
        registry.add("spring.cloud.discovery.client.simple.instances.user-service[0].uri", () -> "http://localhost:18082");
    }

    @BeforeAll
    static void startDependencies() {
        BOOKS.start();
        USERS.start();
    }

    @AfterAll
    static void stopDependencies() {
        BOOKS.stop();
        USERS.stop();
    }

    @Test
    void checksDependenciesAndUpdatesBookAvailabilityWhenCreatingBorrow() {
        USERS.stubFor(get(urlEqualTo("/users/17")).willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"id\":17,\"name\":\"Reader\",\"email\":\"reader@example.test\",\"active\":true}")));
        USERS.stubFor(get(urlEqualTo("/users/18")).willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"id\":18,\"name\":\"Reader 2\",\"email\":\"reader2@example.test\",\"active\":true}")));
        USERS.stubFor(get(urlEqualTo("/users/404")).willReturn(aResponse().withStatus(404)));
        BOOKS.stubFor(get(urlEqualTo("/books/42")).willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"id\":42,\"title\":\"Test book\",\"author\":\"Author\",\"available\":true}")));
        BOOKS.stubFor(get(urlEqualTo("/books/43")).willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"id\":43,\"title\":\"Borrowed book\",\"author\":\"Author\",\"available\":false}")));
        BOOKS.stubFor(patch(urlEqualTo("/books/42/availability")).willReturn(aResponse().withStatus(200)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<BorrowRecord> created = http.exchange(
            "/borrowings", HttpMethod.POST,
            new HttpEntity<>(new BorrowRecord(42L, 17L), headers), BorrowRecord.class
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().getBookId()).isEqualTo(42L);
        assertThat(created.getBody().getUserId()).isEqualTo(17L);
        assertThat(created.getBody().getBorrowDate()).isNotNull();
        assertThat(created.getBody().isReturned()).isFalse();
        BOOKS.verify(1, getRequestedFor(urlEqualTo("/books/42")));
        BOOKS.verify(1, patchRequestedFor(urlEqualTo("/books/42/availability"))
            .withRequestBody(equalToJson("{\"available\":false}")));

        ResponseEntity<String> unavailableBook = http.exchange(
            "/borrowings", HttpMethod.POST,
            new HttpEntity<>(new BorrowRecord(43L, 18L), headers), String.class
        );
        assertThat(unavailableBook.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        BOOKS.verify(1, getRequestedFor(urlEqualTo("/books/43")));

        ResponseEntity<String> missingUser = http.exchange(
            "/borrowings", HttpMethod.POST,
            new HttpEntity<>(new BorrowRecord(42L, 404L), headers), String.class
        );
        assertThat(missingUser.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}