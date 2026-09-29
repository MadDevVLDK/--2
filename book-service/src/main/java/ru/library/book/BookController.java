package ru.library.book;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<Book> getAllBooks() { return bookService.getAllBooks(); }

    @GetMapping("/{id}")
    public Book getBook(@PathVariable("id") Long id) { return bookService.getBook(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Book createBook(@RequestBody Book book) { return bookService.saveBook(book); }

    @PatchMapping("/{id}/availability")
    public Book updateAvailability(@PathVariable("id") Long id, @RequestBody AvailabilityRequest request) {
        return bookService.updateAvailability(id, request.available());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable("id") Long id) { bookService.deleteBook(id); }

    public record AvailabilityRequest(boolean available) {}
}