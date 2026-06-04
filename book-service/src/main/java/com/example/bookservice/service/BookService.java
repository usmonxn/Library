package com.example.bookservice.service;

import com.example.bookservice.entity.BookEntity;
import com.example.bookservice.exception.AccessDeniedException;
import com.example.bookservice.repository.BookRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final ImageCryptoService imageCryptoService;

    public Optional<BookEntity>  getBookById(Long id) {
        return bookRepository.findById(id);
    }
    public List<BookEntity> getAllbooks() {
        List<BookEntity> books = bookRepository.findAll();
        books.forEach(this::normalizeInventory);
        return books;
    }

    public List<String> getGenres() {
        return bookRepository.findDistinctGenres();
    }

    public Page<BookEntity> getCatalogPage(int page, int size, String name, String genre, boolean availableOnly) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 500));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        List<String> genres = parseGenres(genre);
        Page<BookEntity> result = bookRepository.findCatalogPage(
                normalizeQuery(name),
                genres.isEmpty() ? "" : genres.get(0),
                genres.size() < 2 ? "" : genres.get(1),
                genres.isEmpty(),
                availableOnly,
                pageable
        );
        result.getContent().forEach(this::normalizeInventory);
        return result;
    }

    public List<BookEntity>     findByNameAndAuthor(String name){
        return bookRepository.searchByNameOrAuthor(name);
    }
    public BookEntity createBook(BookEntity bookEntity) {
        validateBook(bookEntity);
        normalizeInventory(bookEntity);
        return bookRepository.save(bookEntity);
    }

    public BookEntity updateBook(Long bookId, BookEntity request) {
        validateBook(request);
        BookEntity book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AccessDeniedException(404, "Kitob topilmadi"));
        book.setBook_name(request.getBook_name());
        book.setAuthor(request.getAuthor());
        book.setWrite_year(request.getWrite_year());
        book.setGenre(request.getGenre());
        if (request.getTotalCopies() != null && request.getTotalCopies() > 0) {
            int currentTotal = valueOrDefault(book.getTotalCopies(), 1);
            int currentAvailable = valueOrDefault(book.getAvailableCopies(), currentTotal);
            int borrowed = Math.max(0, currentTotal - currentAvailable);
            book.setTotalCopies(request.getTotalCopies());
            book.setAvailableCopies(Math.max(0, request.getTotalCopies() - borrowed));
        }
        normalizeInventory(book);
        return bookRepository.save(book);
    }

    public BookEntity borrowCopy(Long bookId) {
        BookEntity book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AccessDeniedException(404, "Kitob topilmadi"));
        normalizeInventory(book);
        if (book.getAvailableCopies() <= 0) {
            throw new AccessDeniedException(400, "Bu kitobdan bo'sh nusxa qolmagan");
        }
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        book.setBorrowCount(valueOrDefault(book.getBorrowCount(), 0) + 1);
        normalizeInventory(book);
        return bookRepository.save(book);
    }

    public BookEntity returnCopy(Long bookId) {
        BookEntity book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AccessDeniedException(404, "Kitob topilmadi"));
        normalizeInventory(book);
        book.setAvailableCopies(Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1));
        normalizeInventory(book);
        return bookRepository.save(book);
    }

    public void saveCoverFile(Long bookId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AccessDeniedException(400, "Rasm fayl tanlanishi kerak");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AccessDeniedException(400, "Faqat rasm fayl yuklash mumkin");
        }
        try {
            saveCoverBytes(bookId, file.getBytes(), contentType, file.getOriginalFilename());
        } catch (Exception e) {
            throw new AccessDeniedException(500, "Rasmni saqlab bo'lmadi");
        }
    }

    public void saveCoverFromUrl(Long bookId, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new AccessDeniedException(400, "Rasm linki kiritilishi kerak");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl.trim()))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AccessDeniedException(400, "Rasm linkidan fayl olinmadi");
            }
            String contentType = response.headers()
                    .firstValue("content-type")
                    .orElse("image/jpeg")
                    .split(";")[0];
            if (!contentType.startsWith("image/")) {
                throw new AccessDeniedException(400, "Link rasm faylga olib bormayapti");
            }
            saveCoverBytes(bookId, response.body(), contentType, "remote-cover");
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new AccessDeniedException(500, "Rasm linkidan yuklab bo'lmadi");
        }
    }

    public CoverImage getCoverImage(Long bookId) {
        BookEntity book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AccessDeniedException(404, "Kitob topilmadi"));
        if (book.getCoverImageEncrypted() == null || book.getCoverImageEncrypted().length == 0) {
            throw new AccessDeniedException(404, "Kitob rasmi topilmadi");
        }
        return new CoverImage(
                imageCryptoService.decrypt(book.getCoverImageEncrypted()),
                book.getCoverImageContentType() != null ? book.getCoverImageContentType() : "image/jpeg"
        );
    }

    public void deleteBook(Long BookId){
        if (!bookRepository.existsById(BookId)) {
            throw new AccessDeniedException(404, "Kitob topilmadi");
        }
        bookRepository.deleteById(BookId);
    }

    private void validateBook(BookEntity bookEntity) {
        if (bookEntity.getBook_name() == null || bookEntity.getBook_name().isBlank()) {
            throw new AccessDeniedException(400, "Kitob nomi kiritilishi kerak");
        }
        if (bookEntity.getAuthor() == null || bookEntity.getAuthor().isBlank()) {
            throw new AccessDeniedException(400, "Muallif kiritilishi kerak");
        }
    }

    private void normalizeInventory(BookEntity book) {
        int total = valueOrDefault(book.getTotalCopies(), 1);
        if (total < 1) total = 1;
        int available = book.getAvailableCopies() == null ? total : book.getAvailableCopies();
        available = Math.max(0, Math.min(total, available));
        book.setTotalCopies(total);
        book.setAvailableCopies(available);
        book.setBorrowCount(valueOrDefault(book.getBorrowCount(), 0));
        book.setStatus(available > 0 ? "AVAILABLE" : "BORROWED");
    }

    private int valueOrDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String normalizeQuery(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> parseGenres(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .limit(2)
                .toList();
    }

    private void saveCoverBytes(Long bookId, byte[] bytes, String contentType, String fileName) {
        BookEntity book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AccessDeniedException(404, "Kitob topilmadi"));
        book.setCoverImageEncrypted(imageCryptoService.encrypt(bytes));
        book.setCoverImageContentType(contentType);
        book.setCoverImageFileName(fileName);
        book.setCoverUrl(null);
        bookRepository.save(book);
    }

    public record CoverImage(byte[] bytes, String contentType) {
    }
}
