package ru.library.borrow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "borrow_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Setter
    private Long bookId;

    @Column(nullable = false)
    @Setter
    private Long userId;

    @Column(nullable = false)
    private LocalDate borrowDate;

    @Column(nullable = false)
    private boolean returned;

    public BorrowRecord(Long bookId, Long userId) {
        this.bookId = bookId;
        this.userId = userId;
        this.borrowDate = LocalDate.now();
        this.returned = false;
    }
}