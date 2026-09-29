package ru.library.borrow;

import org.springframework.stereotype.Service;

@Service
public class BorrowService {
    private final BorrowRepository borrowRepository;
    private final LibraryServiceClient libraryServiceClient;

    public BorrowService(BorrowRepository borrowRepository, LibraryServiceClient libraryServiceClient) {
        this.borrowRepository = borrowRepository;
        this.libraryServiceClient = libraryServiceClient;
    }

    public BorrowRecord createBorrowRecord(Long bookId, Long userId) {
        libraryServiceClient.requireActiveUser(userId);
        libraryServiceClient.requireBookAvailable(bookId);
        libraryServiceClient.updateBookAvailability(bookId, false);
        try {
            return borrowRepository.saveAndFlush(new BorrowRecord(bookId, userId));
        } catch (RuntimeException exception) {
            libraryServiceClient.updateBookAvailability(bookId, true);
            throw exception;
        }
    }
}