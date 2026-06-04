package com.example.bookservice.controller;

import com.example.bookservice.entity.BookEntity;
import com.example.bookservice.exception.AccessDeniedException;
import com.example.bookservice.service.BookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/book")
public class BookController {
    private final BookService bookService;
    private final String internalToken;

    public BookController(BookService bookService,
                          @Value("${app.internal-token}") String internalToken) {
        this.bookService = bookService;
        this.internalToken = internalToken;
    }

    @GetMapping(path = "/all-books")
    public List<BookEntity> getBooks() {
        return bookService.getAllbooks();
    }

    @GetMapping(path = "/genres")
    public List<String> getGenres() {
        return bookService.getGenres();
    }

    @GetMapping(path = "/page")
    public Page<BookEntity> getBooksPage(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "8") int size,
                                         @RequestParam(defaultValue = "") String name,
                                         @RequestParam(defaultValue = "") String genre,
                                         @RequestParam(defaultValue = "true") boolean availableOnly) {
        return bookService.getCatalogPage(page, size, name, genre, availableOnly);
    }

    @GetMapping(path = "/{bookId}")
    public BookEntity getBookById(@PathVariable Long bookId) {
        return bookService.getBookById(bookId)
                .orElseThrow(() -> new AccessDeniedException(404, "Kitob topilmadi"));
    }

    @GetMapping(path = "/name")
    public List<BookEntity> getBookById(@RequestParam String name) {
        return bookService.findByNameAndAuthor(name);
    }

    @PostMapping
    public BookEntity createBook(@RequestBody BookEntity bookEntity) {
        return bookService.createBook(bookEntity);
    }

    @PutMapping(path = "/{bookId}")
    public BookEntity updateBook(@PathVariable Long bookId, @RequestBody BookEntity bookEntity) {
        return bookService.updateBook(bookId, bookEntity);
    }

    @PostMapping(path = "/{bookId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadCover(@PathVariable Long bookId, @RequestParam("file") MultipartFile file) {
        bookService.saveCoverFile(bookId, file);
    }

    @PostMapping(path = "/{bookId}/cover-url")
    public void uploadCoverFromUrl(@PathVariable Long bookId, @RequestBody Map<String, String> request) {
        bookService.saveCoverFromUrl(bookId, request.get("imageUrl"));
    }

    @GetMapping(path = "/{bookId}/cover")
    public ResponseEntity<byte[]> getCover(@PathVariable Long bookId) {
        BookService.CoverImage coverImage = bookService.getCoverImage(bookId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(coverImage.contentType()))
                .body(coverImage.bytes());
    }

    @DeleteMapping(path = "/delete/{BookId}")
    public void deleteBook(@PathVariable Long BookId){
        bookService.deleteBook(BookId);
    }

    @PostMapping(path = "/internal/{bookId}/borrow")
    public BookEntity borrowCopy(@PathVariable Long bookId, @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        validateInternalToken(token);
        return bookService.borrowCopy(bookId);
    }

    @PostMapping(path = "/internal/{bookId}/return")
    public BookEntity returnCopy(@PathVariable Long bookId, @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        validateInternalToken(token);
        return bookService.returnCopy(bookId);
    }

    private void validateInternalToken(String token) {
        if (token == null || !token.equals(internalToken)) {
            throw new AccessDeniedException(403, "Ichki servis tokeni noto'g'ri");
        }
    }
}
