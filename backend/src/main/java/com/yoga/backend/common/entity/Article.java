package com.yoga.backend.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 게시글(공지사항) 엔티티 클래스
 */
@Data
@Entity
@Table(name = "Article")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long articleId; // 게시글 ID

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user; // 작성자 (강사)

    private String title; // 게시글 제목
    private String content; // 게시글 내용
    private LocalDateTime createdAt; // 생성일자
    private LocalDateTime updatedAt; // 수정일자
    private String imageUrl; // 이미지 URL

    /**
     * 게시글 생성 전에 호출되어 생성일자를 설정합니다.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 게시글 수정 전에 호출되어 수정일자를 설정합니다.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}