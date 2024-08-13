package com.yoga.backend.recorded.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.yoga.backend.common.entity.RecordedLectures.QRecordedLecture;
import com.yoga.backend.common.entity.RecordedLectures.QRecordedLectureLike;
import com.yoga.backend.recorded.dto.LectureDto;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AllRecordedLecturesRepository {

    private final JPAQueryFactory queryFactory;

    public AllRecordedLecturesRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<LectureDto> findAllLectures(int userId, int page, int size, String sort) {
        QRecordedLecture lecture = QRecordedLecture.recordedLecture;
        QRecordedLectureLike like = QRecordedLectureLike.recordedLectureLike;

        JPQLQuery<LectureDto> query = queryFactory
            .select(Projections.constructor(LectureDto.class,
                lecture.id,
                lecture.user.id.as("userId"),
                lecture.user.nickname.as("nickname"),
                lecture.title,
                lecture.content,
                lecture.thumbnail,
                lecture.thumbnailSmall,
                lecture.likeCount,
                lecture.createdDate,
                lecture.lastModifiedDate,
                JPAExpressions
                    .selectOne()
                    .from(like)
                    .where(like.lecture.eq(lecture).and(like.user.id.eq(userId)))
                    .exists()
            ))
            .from(lecture).where(lecture.user.isDeleted.eq(false));

        // 정렬 적용
        if ("fame".equals(sort)) {
            query.orderBy(lecture.likeCount.desc(), lecture.createdDate.desc());
        } else {
            // "date" 또는 기본 정렬
            query.orderBy(lecture.createdDate.desc());
        }

        // 페이지네이션 적용
        query.offset((long) page * size)
            .limit(size);

        return query.fetch();
    }
}