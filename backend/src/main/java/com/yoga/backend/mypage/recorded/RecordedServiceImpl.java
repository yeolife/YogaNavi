package com.yoga.backend.mypage.recorded;

import com.yoga.backend.common.awsS3.S3Service;
import com.yoga.backend.common.entity.RecordedLectures.RecordedLecture;
import com.yoga.backend.common.entity.RecordedLectures.RecordedLectureChapter;
import com.yoga.backend.common.entity.RecordedLectures.RecordedLectureLike;
import com.yoga.backend.mypage.recorded.dto.ChapterDto;
import com.yoga.backend.mypage.recorded.dto.LectureCreationStatus;
import com.yoga.backend.mypage.recorded.dto.LectureDto;
import com.yoga.backend.mypage.recorded.repository.MyLikeLectureListRepository;
import com.yoga.backend.mypage.recorded.repository.RecordedLectureLikeRepository;
import com.yoga.backend.mypage.recorded.repository.RecordedLectureListRepository;
import com.yoga.backend.mypage.recorded.repository.RecordedLectureRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 녹화 강의 관련 비즈니스 로직을 처리하는 서비스 구현 클래스. 이 클래스는 강의 목록 조회, 강의 생성, 수정, 삭제 및 좋아요 기능을 제공.
 */
@Service
public class RecordedServiceImpl implements RecordedService {

    private static final String S3_BASE_URL = "https://yoga-navi.s3.ap-northeast-2.amazonaws.com/";
    private static final long URL_EXPIRATION_SECONDS = 86400; // 1 hour

    @Autowired
    private RecordedLectureListRepository recordedLectureListRepository;

    @Autowired
    private RecordedLectureRepository recordedLectureRepository;

    @Autowired
    private MyLikeLectureListRepository myLikeLectureListRepository;

    @Autowired
    private RecordedLectureLikeRepository lectureLikeRepository;

    @Autowired
    private S3Service s3Service;

    /**
     * 사용자가 업로드한 강의 목록을 조회
     *
     * @param userId 사용자 id
     * @return 사용자가 업로드한 강의 목록 (LectureDto 리스트)
     */
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<LectureDto> getMyLectures(int userId) {
        List<LectureDto> lectures = recordedLectureListRepository.findAllLectures(userId);
        return applyPresignedUrls(lectures);
    }

    /**
     * 사용자가 좋아요한 강의 목록을 조회
     *
     * @param userId 사용자 id
     * @return 사용자가 업로드한 강의 목록 (LectureDto 리스트)
     */
    @Override
    @Transactional(readOnly = true)
    public List<LectureDto> getLikeLectures(int userId) {
        List<LectureDto> lectures = myLikeLectureListRepository.findMyLikedLectures(userId);
        return generatePresignedUrlsLike(lectures);
    }

    /**
     * 강의를 저장
     *
     * @param lectureDto 강의 정보
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public void saveLecture(LectureDto lectureDto) {
        RecordedLecture lecture = new RecordedLecture();
        lecture.setUserId(lectureDto.getUserId());
        lecture.setTitle(lectureDto.getRecordTitle());
        lecture.setContent(lectureDto.getRecordContent());
        lecture.setThumbnail(lectureDto.getRecordThumbnail());

        List<RecordedLectureChapter> chapters = new ArrayList<>();
        for (ChapterDto chapterDto : lectureDto.getRecordedLectureChapters()) {
            RecordedLectureChapter chapter = new RecordedLectureChapter();
            chapter.setTitle(chapterDto.getChapterTitle());
            chapter.setDescription(chapterDto.getChapterDescription());
            chapter.setVideoUrl(chapterDto.getRecordVideo());
            chapter.setLecture(lecture);
            chapter.setChapterNumber(chapterDto.getChapterNumber());
            chapters.add(chapter);
        }
        lecture.setChapters(chapters);
        recordedLectureRepository.save(lecture);
    }


    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public LectureDto getLectureDetails(Long recordedId, int userId) {
        RecordedLecture lecture = recordedLectureRepository.findById(recordedId)
            .orElseThrow(() -> new RuntimeException("Lecture not found"));

        LectureDto dto = convertToDto(lecture);
        dto.setLikeCount(lecture.getLikeCount());
        dto.setMyLike(lectureLikeRepository.existsByLectureIdAndUserId(recordedId, userId));

        // Presigned URL 생성 및 적용
        return applyPresignedUrls(Collections.singletonList(dto)).get(0);
    }

    @Override
    @Transactional
    public LectureDto updateLecture(Long lectureId, LectureDto lectureDto, int userId) {
        RecordedLecture lecture = recordedLectureRepository.findById(lectureId)
            .orElseThrow(() -> new RuntimeException("강의가 없습니다."));

        if (lecture.getUserId() != userId) {
            throw new RuntimeException("강의를 수정할 수 없습니다.");
        }

        updateLectureDetails(lecture, lectureDto);
        updateChapters(lecture, lectureDto.getRecordedLectureChapters());

        RecordedLecture updatedLecture = recordedLectureRepository.save(lecture);
        return convertToDto(updatedLecture);
    }

    private void updateLectureDetails(RecordedLecture lecture, LectureDto lectureDto) {
        lecture.setTitle(lectureDto.getRecordTitle());
        lecture.setContent(lectureDto.getRecordContent());

        if (!lecture.getThumbnail().equals(lectureDto.getRecordThumbnail())) {
            String oldThumbnail = lecture.getThumbnail();
            lecture.setThumbnail(lectureDto.getRecordThumbnail());
            s3Service.deleteFile(oldThumbnail);
        }
    }

    private void updateChapters(RecordedLecture lecture, List<ChapterDto> chapterDtos) {
        Map<Long, RecordedLectureChapter> existingChapters = lecture.getChapters().stream()
            .collect(Collectors.toMap(RecordedLectureChapter::getId, Function.identity()));

        List<RecordedLectureChapter> updatedChapters = new ArrayList<>();

        for (ChapterDto chapterDto : chapterDtos) {
            RecordedLectureChapter chapter = existingChapters.get(chapterDto.getId());
            if (chapter == null) {
                chapter = new RecordedLectureChapter();
                chapter.setLecture(lecture);
            }

            updateChapter(chapter, chapterDto);
            updatedChapters.add(chapter);
            existingChapters.remove(chapter.getId());
        }

        // 삭제된 챕터 처리
        for (RecordedLectureChapter removedChapter : existingChapters.values()) {
            s3Service.deleteFile(removedChapter.getVideoUrl());
        }

        lecture.setChapters(updatedChapters);
    }

    private void updateChapter(RecordedLectureChapter chapter, ChapterDto chapterDto) {
        chapter.setTitle(chapterDto.getChapterTitle());
        chapter.setDescription(chapterDto.getChapterDescription());
        chapter.setChapterNumber(chapterDto.getChapterNumber());

        if (!chapter.getVideoUrl().equals(chapterDto.getRecordVideo())) {
            String oldVideo = chapter.getVideoUrl();
            chapter.setVideoUrl(chapterDto.getRecordVideo());
            s3Service.deleteFile(oldVideo);
        }
    }

    @Override
    @Transactional
    public void deleteLecture(Long lectureId, int userId) {
        RecordedLecture lecture = recordedLectureRepository.findById(lectureId)
            .orElseThrow(() -> new RuntimeException("강의가 없습니다."));

        if (lecture.getUserId() != userId) {
            throw new RuntimeException("강의를 삭제할 수 없습니다.");
        }

        // 강의 삭제
        recordedLectureRepository.delete(lecture);

        // S3에서 파일 삭제
        s3Service.deleteFile(lecture.getThumbnail());
        lecture.getChapters().forEach(chapter -> {
            s3Service.deleteFile(chapter.getVideoUrl());
        });
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public LectureDto setLike(Long recordedId, int userId) {
        return recordedLectureRepository.findById(recordedId)
            .map(lecture -> {
                if (!lectureLikeRepository.existsByLectureIdAndUserId(recordedId, userId)) {
                    RecordedLectureLike like = new RecordedLectureLike();
                    like.setLecture(lecture);
                    like.setUserId(userId);
                    lectureLikeRepository.save(like);
                    lectureLikeRepository.save(like);

                    boolean updated = false;
                    while (!updated) {
                        try {
                            lecture.incrementLikeCount();
                            recordedLectureRepository.save(lecture);
                            updated = true;
                        } catch (OptimisticLockingFailureException e) {
                            // Refresh the entity and retry
                            lecture = recordedLectureRepository.findById(recordedId)
                                .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다."));
                        }
                    }
                }
                return getLectureDetails(recordedId, userId);
            })
            .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다."));
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public LectureDto setDislike(Long recordedId, int userId) {
        return recordedLectureRepository.findById(recordedId)
            .map(lecture -> {
                if (lectureLikeRepository.existsByLectureIdAndUserId(recordedId, userId)) {
                    lectureLikeRepository.deleteByLectureIdAndUserId(recordedId, userId);

                    boolean updated = false;
                    while (!updated) {
                        try {
                            lecture.decrementLikeCount();
                            recordedLectureRepository.save(lecture);
                            updated = true;
                        } catch (OptimisticLockingFailureException e) {
                            // Refresh the entity and retry
                            lecture = recordedLectureRepository.findById(recordedId)
                                .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다."));
                        }
                    }
                }
                return getLectureDetails(recordedId, userId);
            })
            .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다."));
    }

    private LectureDto convertToDto(RecordedLecture lecture) {
        LectureDto dto = new LectureDto();
        dto.setRecordedId(lecture.getId());
        dto.setUserId(lecture.getUserId());
        dto.setRecordTitle(lecture.getTitle());
        dto.setRecordContent(lecture.getContent());
        dto.setRecordThumbnail(lecture.getThumbnail());
        dto.setLikeCount(lecture.getLikeCount());
        dto.setMyLike(false); // 이 값은 나중에 설정됩니다.

        List<ChapterDto> chapterDtos = new ArrayList<>();
        for (RecordedLectureChapter chapter : lecture.getChapters()) {
            ChapterDto chapterDto = new ChapterDto();
            chapterDto.setId(chapter.getId());
            chapterDto.setChapterTitle(chapter.getTitle());
            chapterDto.setChapterDescription(chapter.getDescription());
            chapterDto.setChapterNumber(chapter.getChapterNumber());
            chapterDto.setRecordVideo(chapter.getVideoUrl());
            chapterDtos.add(chapterDto);
        }
        dto.setRecordedLectureChapters(chapterDtos);

        return dto;
    }

    private List<LectureDto> generatePresignedUrlsLike(List<LectureDto> lectures) {
        for (LectureDto lecture : lectures) {
            lecture.setRecordThumbnail(s3Service.generatePresignedUrl(lecture.getRecordThumbnail(),
                3600)); // 1 hour expiration
        }
        return lectures;
    }

    private List<LectureDto> applyPresignedUrls(List<LectureDto> lectures) {
        Map<String, String> presignedUrls = generatePresignedUrls(lectures);

        for (LectureDto lecture : lectures) {
            lecture.setRecordThumbnail(
                getPresignedUrl(lecture.getRecordThumbnail(), presignedUrls));
            if (lecture.getRecordedLectureChapters() != null) {
                for (ChapterDto chapter : lecture.getRecordedLectureChapters()) {
                    chapter.setRecordVideo(
                        getPresignedUrl(chapter.getRecordVideo(), presignedUrls));
                }
            }
        }

        return lectures;
    }

    private Map<String, String> generatePresignedUrls(List<LectureDto> lectures) {
        Set<String> keysToGenerate = new HashSet<>();

        for (LectureDto lecture : lectures) {
            addKeyIfNeeded(keysToGenerate, lecture.getRecordThumbnail());
            if (lecture.getRecordedLectureChapters() != null) {
                for (ChapterDto chapter : lecture.getRecordedLectureChapters()) {
                    addKeyIfNeeded(keysToGenerate, chapter.getRecordVideo());
                }
            }
        }

        if (keysToGenerate.isEmpty()) {
            return Collections.emptyMap();
        }

        return s3Service.generatePresignedUrls(keysToGenerate, URL_EXPIRATION_SECONDS);
    }


    private void addKeyIfNeeded(Set<String> keysToGenerate, String url) {
        if (url != null && url.startsWith(S3_BASE_URL)) {
            String key = url.substring(S3_BASE_URL.length());
            keysToGenerate.add(key);
        }
    }

    private String getPresignedUrl(String url, Map<String, String> presignedUrls) {
        if (url != null && url.startsWith(S3_BASE_URL)) {
            String key = url.substring(S3_BASE_URL.length());
            return presignedUrls.getOrDefault(key, url);
        }
        return url;
    }

}