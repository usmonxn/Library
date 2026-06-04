package com.example.bookservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Data
@Table(name = "books")
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String author;
    private String book_name;
    private String write_year;
    private String genre;
    private String created_at;
    @Column(name = "total_copies")
    private Integer totalCopies;
    @Column(name = "available_copies")
    private Integer availableCopies;
    @Column(name = "borrow_count")
    private Integer borrowCount;
    private String status;
    @Column(name = "cover_url", length = 1000)
    private String coverUrl;
    @JsonIgnore
    @Column(name = "cover_image_encrypted", columnDefinition = "bytea")
    private byte[] coverImageEncrypted;
    @Column(name = "cover_image_content_type")
    private String coverImageContentType;
    @Column(name = "cover_image_file_name")
    private String coverImageFileName;

    public Boolean getHasCoverImage() {
        return coverImageEncrypted != null && coverImageEncrypted.length > 0;
    }

}
