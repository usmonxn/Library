package com.example.bookservice.repository;

import com.example.bookservice.entity.BookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {


    @Query(nativeQuery = true, value = """
            SELECT b.*
            FROM books b
            WHERE lower(concat(author,book_name))
                  LIKE lower(concat('%', :name, '%'))
            """)
    List<BookEntity> searchByNameOrAuthor(@Param("name") String name);

    @Query("""
            select b from BookEntity b
            where (:name is null or :name = '' or
                   lower(concat(coalesce(b.author, ''), ' ', coalesce(b.book_name, ''), ' ', coalesce(b.genre, '')))
                   like lower(concat('%', :name, '%')))
              and (:genresEmpty = true or b.genre = :genreOne or b.genre = :genreTwo)
              and (:availableOnly = false or coalesce(b.availableCopies, 1) > 0)
            """)
    Page<BookEntity> findCatalogPage(@Param("name") String name,
                                     @Param("genreOne") String genreOne,
                                     @Param("genreTwo") String genreTwo,
                                     @Param("genresEmpty") boolean genresEmpty,
                                     @Param("availableOnly") boolean availableOnly,
                                     Pageable pageable);

    @Query("""
            select distinct b.genre from BookEntity b
            where b.genre is not null and trim(b.genre) <> ''
            order by b.genre
            """)
    List<String> findDistinctGenres();

}
