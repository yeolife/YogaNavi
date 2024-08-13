package com.yoga.backend.livelectures.service;

import com.yoga.backend.common.entity.LiveLectures;
import com.yoga.backend.common.entity.MyLiveLecture;
import com.yoga.backend.common.entity.Users;
import com.yoga.backend.livelectures.dto.HomeResponseDto;
import com.yoga.backend.livelectures.repository.LiveLectureRepository;
import com.yoga.backend.livelectures.repository.MyLiveLectureRepository;
import com.yoga.backend.members.repository.UsersRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내가 듣거나 진행할 강의들
 */
@Slf4j
@Service
public class HomeServiceImpl implements HomeService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private final LiveLectureRepository liveLectureRepository;
    private final MyLiveLectureRepository myLiveLectureRepository;
    private final UsersRepository usersRepository;

    public HomeServiceImpl(
        LiveLectureRepository liveLectureRepository,
        MyLiveLectureRepository myLiveLectureRepository,
        UsersRepository usersRepository) {
        this.liveLectureRepository = liveLectureRepository;
        this.myLiveLectureRepository = myLiveLectureRepository;
        this.usersRepository = usersRepository;
    }

    /**
     * 사용자의 진행/수강할 강의 조회
     *
     * @param userId 사용자 ID
     * @return 강의 이력 DTO 리스트
     */
    @Override
    @Transactional(readOnly = true)
    public List<HomeResponseDto> getHomeData(int userId, int page, int size) {
        ZonedDateTime nowKorea = ZonedDateTime.now(KOREA_ZONE);
        List<HomeResponseDto> result = new ArrayList<>();
        result.addAll(getUserLectures(userId, nowKorea));
        result.addAll(getStudentLectures(userId, nowKorea));
        List<HomeResponseDto> sortedResult = sortHomeData(result);
        // 페이지네이션
        int start = page * size;
        if (start >= sortedResult.size()) {
            log.warn("사용자 ID: {}, 페이지: {} - 요청된 페이지가 데이터 범위를 벗어남. 빈 리스트 반환", userId, page);
            return Collections.emptyList();
        }
        int end = Math.min((start + size), sortedResult.size());
        log.info("사용자 ID: {}, 페이지: {}, 크기: {} - 데이터 부분 집합 반환: {} ~ {}", userId, page, size, start,
            end);
        return sortedResult.subList(start, end);
    }

    /**
     * 학생의 수강할 강의들
     */
    @Transactional(readOnly = true)
    protected List<HomeResponseDto> getStudentLectures(int userId, ZonedDateTime nowKorea) {
        log.debug("학생 강의 조회 시작: 사용자 ID {}", userId);
        LocalDate currentDate = nowKorea.toLocalDate();
        List<MyLiveLecture> myLiveLectures = myLiveLectureRepository.findCurrentMyLectures(
            userId, currentDate);

        List<HomeResponseDto> result = new ArrayList<>();
        for (MyLiveLecture myLiveLecture : myLiveLectures) {
            LiveLectures lecture = myLiveLecture.getLiveLecture();
            List<HomeResponseDto> dtos = convertToHomeResponseDto(lecture, myLiveLecture,
                nowKorea, false);
            for (HomeResponseDto dto : dtos) {
                Users teacher = lecture.getUser();
                dto.setProfileImageUrl(teacher.getProfile_image_url());
                dto.setProfileImageUrlSmall(teacher.getProfile_image_url_small());
                result.add(dto);
            }
        }
        log.debug("학생 강의 변환 완료: 사용자 ID {}, 강의 수 {}", userId, result.size());
        return result;
    }

    /**
     * 강사가 강의 할 것들
     */
    @Transactional(readOnly = true)
    protected List<HomeResponseDto> getUserLectures(int userId, ZonedDateTime nowKorea) {
        log.debug("강사 강의 조회 시작: 사용자 ID {}", userId);
        LocalDate currentDate = nowKorea.toLocalDate();
        List<LiveLectures> lectures = liveLectureRepository.findLecturesByUserAndDateRange(userId,
            currentDate);
        Users user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        List<HomeResponseDto> result = new ArrayList<>();
        for (LiveLectures lecture : lectures) {
            List<HomeResponseDto> dtos = convertToHomeResponseDto(lecture, null, nowKorea,
                true);
            for (HomeResponseDto dto : dtos) {
                // 강사의 프로필 이미지 URL 설정
                dto.setProfileImageUrl(user.getProfile_image_url());
                dto.setProfileImageUrlSmall(user.getProfile_image_url_small());
                result.add(dto);
            }
        }
        log.debug("강사 강의 변환 완료: 사용자 ID {}, 강의 수 {}", userId, result.size());
        return result;
    }

    /**
     * LiveLectures Entity -> HomeResponseDto
     */
    private List<HomeResponseDto> convertToHomeResponseDto(LiveLectures lecture,
        MyLiveLecture myLiveLecture, ZonedDateTime nowKorea, boolean isTeacher) {
        List<HomeResponseDto> dtos = new ArrayList<>();
        LocalDate startDate;
        LocalDate endDate;
        if (myLiveLecture != null) {
            startDate = myLiveLecture.getStartDate().atZone(ZoneOffset.UTC)
                .withZoneSameInstant(KOREA_ZONE).toLocalDate();
            endDate = myLiveLecture.getEndDate().atZone(ZoneOffset.UTC)
                .withZoneSameInstant(KOREA_ZONE).toLocalDate();
        } else {
            startDate = lecture.getStartDate().atZone(ZoneOffset.UTC)
                .withZoneSameInstant(KOREA_ZONE).toLocalDate();
            endDate = lecture.getEndDate().atZone(ZoneOffset.UTC)
                .withZoneSameInstant(KOREA_ZONE).toLocalDate();
        }

        LocalTime startTime = ZonedDateTime.ofInstant(lecture.getStartTime(), ZoneId.of("UTC"))
            .toLocalTime();
        LocalTime endTime = ZonedDateTime.ofInstant(lecture.getEndTime(), ZoneId.of("UTC"))
            .toLocalTime();
        LocalDate today = nowKorea.toLocalDate();
        LocalTime nowTime = nowKorea.toLocalTime();
        boolean overNight = endTime.isBefore(startTime);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (lecture.getAvailableDay()
                .contains(date.getDayOfWeek().toString().substring(0, 3))) {
                // 오늘 날짜 강의 && 종료 시간이 현재 시간 이후 || 자정을 넘음
                boolean isLectureToday =
                    date.isEqual(today) && (endTime.isAfter(nowTime) || overNight);
                // 어제 시작된 강의가 오늘까지 이어짐 (자정을 넘고, 현재 시간이 종료 시간 이전)
                boolean isLectureStartedYesterday =
                    date.equals(today.minusDays(1)) && overNight && endTime.isAfter(nowTime);
                // 미래 강의
                boolean isFutureLecture = date.isAfter(today);
                if (isFutureLecture || isLectureToday || isLectureStartedYesterday) {
                    boolean isOnAir = false;
                    if (isLectureStartedYesterday) { // 어제 시작된 강의가 오늘까지 이어짐
                        isOnAir = lecture.getIsOnAir();
                    } else if (isLectureToday) { // 오늘 날짜 강의
                        isOnAir = lecture.getIsOnAir();
                    }
                    HomeResponseDto dto = createHomeResponseDto(lecture, date, startTime, endTime,
                        isTeacher, isOnAir);
                    dtos.add(dto);
                }
            }
        }
        return dtos;
    }

    /**
     * HomeResponseDto 생성
     */
    private HomeResponseDto createHomeResponseDto(LiveLectures lecture, LocalDate date,
        LocalTime startTime, LocalTime endTime, boolean isTeacher, boolean isOnAir) {
        HomeResponseDto dto = new HomeResponseDto();
        dto.setLiveId(lecture.getLiveId());
        dto.setNickname(lecture.getUser().getNickname());
        dto.setLiveTitle(lecture.getLiveTitle());
        dto.setLiveContent(lecture.getLiveContent());
        ZonedDateTime lectureDateTime = date.atStartOfDay(KOREA_ZONE);
        ZonedDateTime gmtLectureDateTime = lectureDateTime.withZoneSameInstant(ZoneOffset.UTC);
        dto.setLectureDate(gmtLectureDateTime.toInstant().toEpochMilli());
        ZonedDateTime startDateTime = date.atTime(startTime).atZone(ZoneId.of("UTC"));
        ZonedDateTime endDateTime = date.atTime(endTime).atZone(ZoneId.of("UTC"));
        dto.setStartTime(
            startDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalTime().toNanoOfDay()
                / 1_000_000);
        dto.setEndTime(endDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalTime().toNanoOfDay()
            / 1_000_000);
        dto.setLectureDay(date.getDayOfWeek().toString().substring(0, 3));
        dto.setMaxLiveNum(lecture.getMaxLiveNum());
        dto.setProfileImageUrl(lecture.getUser().getProfile_image_url());
        dto.setProfileImageUrlSmall(lecture.getUser().getProfile_image_url_small());
        dto.setTeacher(isTeacher);
        dto.setIsOnAir(lecture.getIsOnAir());
        dto.setIsOnAir(isOnAir);
        return dto;
    }

    /**
     * 오름차순 정렬
     */
    private List<HomeResponseDto> sortHomeData(List<HomeResponseDto> data) {
        return data.stream()
            .sorted(Comparator
                .comparingLong(HomeResponseDto::getLectureDate)
                .thenComparingLong(HomeResponseDto::getStartTime))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public boolean updateLiveState(Long liveId, Boolean isOnAir) {
        if (liveId == null) {
            throw new IllegalArgumentException("liveId가 null입니다.");
        }
        try {
            LiveLectures liveLectures = liveLectureRepository.findById(liveId)
                .orElseThrow(() -> new NullPointerException("강의를 찾을 수 없습니다: " + liveId));
            liveLectures.setIsOnAir(isOnAir);
            liveLectureRepository.save(liveLectures);
            return true;
        } catch (Exception e) {
            log.error("라이브 상태 업데이트 중 에러 발생. liveId : {}", liveId, e);
            throw new RuntimeException("라이브 상태 업데이트 중 오류가 발생했습니다.", e);
        }
    }
}