package ru.library.borrow;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Configuration
class LibraryClientConfiguration {
    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}

@Component
class LibraryServiceClient {
    private final RestClient restClient;

    LibraryServiceClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    void requireActiveUser(Long userId) {
        try {
            UserResponse user = restClient.get()
                    .uri("http://user-service/users/{id}", userId)
                    .retrieve()
                    .body(UserResponse.class);
            if (user == null || !user.active()) {
                throw new ResponseStatusException(HttpStatusCode.valueOf(404), "Active user not found");
            }
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatusCode.valueOf(404), "User not found");
            }
            throw new ResponseStatusException(HttpStatusCode.valueOf(503), "User service is unavailable");
        }
    }

    void requireBookAvailable(Long bookId) {
        try {
            BookResponse book = restClient.get()
                    .uri("http://book-service/books/{id}", bookId)
                    .retrieve()
                    .body(BookResponse.class);
            if (book == null) {
                throw new ResponseStatusException(HttpStatusCode.valueOf(404), "Book not found");
            }
            if (!book.available()) {
                throw new ResponseStatusException(HttpStatusCode.valueOf(409), "Book is not available");
            }
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResponseStatusException(exception.getStatusCode(), "Book not found");
            }
            throw new ResponseStatusException(HttpStatusCode.valueOf(503), "Book service is unavailable");
        }
    }

    void updateBookAvailability(Long bookId, boolean available) {
        restClient.patch()
                .uri("http://book-service/books/{id}/availability", bookId)
                .body(new AvailabilityRequest(available))
                .retrieve()
                .toBodilessEntity();
    }

    record BookResponse(Long id, String title, String author, boolean available) {}
    record AvailabilityRequest(boolean available) {}
    record UserResponse(Long id, String name, String email, boolean active) {}
}