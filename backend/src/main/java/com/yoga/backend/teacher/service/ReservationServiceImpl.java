package com.yoga.backend.teacher.service;

import com.yoga.backend.common.entity.LiveLectures;
import com.yoga.backend.common.entity.MyLiveLecture;
import com.yoga.backend.common.entity.Users;
import com.yoga.backend.livelectures.repository.LiveLectureRepository;
import com.yoga.backend.livelectures.repository.MyLiveLectureRepository;
import com.yoga.backend.members.repository.UsersRepository;
import com.yoga.backend.livelectures.dto.LiveLectureDto;
import com.yoga.backend.teacher.dto.ReservationRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 예약 서비스 구현. 예약 생성 및 조회 등의 비즈니스 로직 구현
 */
@Service
public class ReservationServiceImpl implements ReservationService {

    private final MyLiveLectureRepository myLiveLectureRepository;
    private final UsersRepository usersRepository;
    private final LiveLectureRepository liveLectureRepository;

    @Autowired
    public ReservationServiceImpl(MyLiveLectureRepository myLiveLectureRepository,
        UsersRepository usersRepository,
        LiveLectureRepository liveLectureRepository) {
        this.myLiveLectureRepository = myLiveLectureRepository;
        this.usersRepository = usersRepository;
        this.liveLectureRepository = liveLectureRepository;
    }

    /**
     * 예약 생성
     *
     * @param userId             사용자 ID
     * @param reservationRequest 예약 요청 DTO
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createReservation(int userId, ReservationRequestDto reservationRequest) {
        Users user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        LiveLectures newLiveLecture = liveLectureRepository.findById(
                (long) reservationRequest.getLiveId())
            .orElseThrow(() -> new RuntimeException("실시간 강의를 찾을 수 없습니다."));

        // 사용자가 자신의 강의를 예약하는 것을 막음
        if (newLiveLecture.getUser().getId() == userId) {
            throw new RuntimeException("자신의 강의는 예약할 수 없습니다.");
        }

        // 최대 인원수 체크
        int currentParticipants = myLiveLectureRepository.countByLiveLectureAndEndDateAfter(
            newLiveLecture, Instant.now());
        if (currentParticipants >= newLiveLecture.getMaxLiveNum()) {
            throw new RuntimeException("최대 인원을 초과했습니다.");
        }

        // 시간 겹침 체크
        ZonedDateTime newStartDateTime = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(reservationRequest.getStartDate()), ZoneId.of("UTC"));
        ZonedDateTime newEndDateTime = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(reservationRequest.getEndDate()), ZoneId.of("UTC"));

        List<MyLiveLecture> userReservations = myLiveLectureRepository.findByUserId(userId);

        for (MyLiveLecture existingReservation : userReservations) {
            ZonedDateTime existingStartDateTime = existingReservation.getStartDate()
                .atZone(ZoneId.of("UTC"));
            ZonedDateTime existingEndDateTime = existingReservation.getEndDate()
                .atZone(ZoneId.of("UTC"));

            if (isDateOverlap(newStartDateTime, newEndDateTime, existingStartDateTime,
                existingEndDateTime) &&
                isTimeOverlap(newLiveLecture, existingReservation.getLiveLecture())) {
                throw new RuntimeException("시간이 겹치는 강의가 이미 존재합니다.");
            }
        }

        MyLiveLecture myLiveLecture = new MyLiveLecture();
        myLiveLecture.setUser(user);
        myLiveLecture.setLiveLecture(newLiveLecture);
        myLiveLecture.setStartDate(newStartDateTime.toInstant());
        myLiveLecture.setEndDate(newEndDateTime.toInstant());

        myLiveLectureRepository.save(myLiveLecture);
    }

    /**
     * 날짜가 겹치는지 확인하는 메서드
     *
     * @param start1 첫 번째 기간의 시작 시간
     * @param end1   첫 번째 기간의 종료 시간
     * @param start2 두 번째 기간의 시작 시간
     * @param end2   두 번째 기간의 종료 시간
     * @return 날짜가 겹치는지 여부
     */
    private boolean isDateOverlap(ZonedDateTime start1, ZonedDateTime end1, ZonedDateTime start2,
        ZonedDateTime end2) {
        return (start1.isBefore(end2) && end1.isAfter(start2)) ||
            start1.equals(start2) || end1.equals(end2);
    }

    /**
     * 시간이 겹치는지 확인하는 메서드
     *
     * @param lecture1 첫 번째 강의
     * @param lecture2 두 번째 강의
     * @return 시간이 겹치는지 여부
     */
    private boolean isTimeOverlap(LiveLectures lecture1, LiveLectures lecture2) {
        Set<String> days1 = new HashSet<>(Arrays.asList(lecture1.getAvailableDay().split(",")));
        Set<String> days2 = new HashSet<>(Arrays.asList(lecture2.getAvailableDay().split(",")));

        // 요일이 겹치는지 확인
        for (String day : days1) {
            if (days2.contains(day)) {
                LocalTime start1 = LocalTime.ofInstant(lecture1.getStartTime(),
                    ZoneId.systemDefault());
                LocalTime end1 = LocalTime.ofInstant(lecture1.getEndTime(), ZoneId.systemDefault());
                LocalTime start2 = LocalTime.ofInstant(lecture2.getStartTime(),
                    ZoneId.systemDefault());
                LocalTime end2 = LocalTime.ofInstant(lecture2.getEndTime(), ZoneId.systemDefault());

                if ((start1.isBefore(end2) && end1.isAfter(start2)) ||
                    start1.equals(start2) || end1.equals(end2)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 사용자 예약 조회
     *
     * @param userId 사용자 ID
     * @return 사용자의 예약 목록
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<MyLiveLecture> getUserReservations(int userId) {
        return myLiveLectureRepository.findByUserId(userId);
    }

    /**
     * 실시간 강의 예약 조회
     *
     * @param liveId 실시간 강의 ID
     * @return 실시간 강의의 예약 목록
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<MyLiveLecture> getLiveLectureReservations(int liveId) {
        return myLiveLectureRepository.findByLiveLecture_LiveId((long) liveId);
    }

    /**
     * 모든 실시간 강의 조회
     *
     * @param method 조회 방법 (0: 최대 수강자 수가 1인 강의, 1: 그 외)
     * @return 실시간 강의 목록
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<LiveLectureDto> getAllLiveLectures(int method) {
        Instant now = Instant.now();
        List<LiveLectures> lectures;

        if (method == 0) {
            lectures = liveLectureRepository.findAllByMaxLiveNumAndEndDateAfter(1, now);
        } else {
            lectures = liveLectureRepository.findAllByMaxLiveNumGreaterThanAndEndDateAfter(1, now);
        }

        return lectures.stream()
            .filter(lecture -> {
                int currentParticipants = myLiveLectureRepository.countByLiveLectureAndEndDateAfter(
                    lecture, now);
                return currentParticipants < lecture.getMaxLiveNum(); // 현재 참여자 수 체크
            })
            .map(LiveLectureDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * 강사별 실시간 강의 조회
     *
     * @param teacherId 강사 ID
     * @param method    조회 방법 (0: 최대 수강자 수가 1인 강의, 1: 그 외)
     * @return 강사의 실시간 강의 목록
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<LiveLectureDto> getLiveLecturesByTeacherAndMethod(int teacherId, int method) {
        Instant now = Instant.now();
        List<LiveLectures> lectures;

        if (method == 0) {
            lectures = liveLectureRepository.findByUserIdAndMaxLiveNumAndEndDateAfter(teacherId, 1,
                now);
        } else {
            lectures = liveLectureRepository.findByUserIdAndMaxLiveNumGreaterThanAndEndDateAfter(
                teacherId, 1, now);
        }

        return lectures.stream()
            .filter(lecture -> {
                int currentParticipants = myLiveLectureRepository.countByLiveLectureAndEndDateAfter(
                    lecture, now);
                return currentParticipants < lecture.getMaxLiveNum(); // 현재 참여자 수 체크
            })
            .map(LiveLectureDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * 강사 예약 조회
     *
     * @param teacherId 강사 ID
     * @return 강사의 예약 목록
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<MyLiveLecture> getReservationsByTeacher(int teacherId) {
        return myLiveLectureRepository.findByLiveLectureWithUser((long) teacherId);
    }
}
