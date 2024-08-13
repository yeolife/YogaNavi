package com.yoga.backend.common.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yoga.backend.common.converter.InstantToSqlDateConverter;
import com.yoga.backend.common.converter.InstantToSqlTimeConverter;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * 실시간 강의 엔티티 클래스 강의의 제목, 내용, 시작 및 종료 시간 등을 포함
 */
@Getter
@Setter
@Entity
@Table(name = "live_lectures")
public class LiveLectures {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long liveId; // 강의 ID (Primary Key)

    @Column(length = 30, nullable = false)
    private String liveTitle; // 강의 제목

    @Column(length = 300)
    private String liveContent; // 강의 내용

    @Column(nullable = false, columnDefinition = "DATE")
    @Convert(converter = InstantToSqlDateConverter.class)
    private Instant startDate; // 시작 날짜

    @Column(nullable = false, columnDefinition = "DATE")
    @Convert(converter = InstantToSqlDateConverter.class)
    private Instant endDate; // 종료 날짜

    @Column(nullable = false, columnDefinition = "TIME")
    @Convert(converter = InstantToSqlTimeConverter.class)
    private Instant startTime; // 강의 시작 시간

    @Column(nullable = false, columnDefinition = "TIME")
    @Convert(converter = InstantToSqlTimeConverter.class)
    private Instant endTime; // 강의 종료 시간

    @Column(nullable = false)
    private Integer maxLiveNum; // 최대 수강자 수

    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    private Instant regDate; // 강의 등록 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user; // 강사 ID (Foreign Key)

    @Column(length = 100, nullable = false)
    private String availableDay; // 가능한 강의 요일

    @Column(nullable = false)
    private Boolean isOnAir = false; // 강의 오픈 여부

}
