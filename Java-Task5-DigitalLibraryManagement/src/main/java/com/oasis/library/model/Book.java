package com.oasis.library.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Author is required")
    @Column(nullable = false)
    private String author;

    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^[0-9Xx-]{10,17}$", message = "ISBN must be 10-13 digits (hyphens allowed)")
    @Column(nullable = false, unique = true)
    private String isbn;

    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Max(value = 1000, message = "Quantity is too large")
    private Integer quantity;

    /** Copies currently on the shelf. */
    private Integer available;

    @Min(1000) @Max(2100)
    private Integer publishedYear;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    private LocalDateTime addedAt = LocalDateTime.now();

    public Book() {}

    public Book(String title, String author, String isbn, String category, int quantity, int year, String description) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.quantity = quantity;
        this.available = quantity;
        this.publishedYear = year;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getAvailable() { return available; }
    public void setAvailable(Integer available) { this.available = available; }
    public Integer getPublishedYear() { return publishedYear; }
    public void setPublishedYear(Integer publishedYear) { this.publishedYear = publishedYear; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getAddedAt() { return addedAt; }

    public int getIssuedCount() { return quantity - available; }

    /** Deterministic hue so each book gets its own generated cover colour. */
    public int getHue() {
        return Math.abs((title + author).hashCode()) % 360;
    }
}
