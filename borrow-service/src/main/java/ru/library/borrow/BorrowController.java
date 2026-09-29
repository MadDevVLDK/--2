package ru.library.borrow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/borrowings")
public class BorrowController {
    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) { this.borrowService = borrowService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowRecord borrow(@RequestBody BorrowRecord request) {
        return borrowService.createBorrowRecord(request.getBookId(), request.getUserId());
    }
}