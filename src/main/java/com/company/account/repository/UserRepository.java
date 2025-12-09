package com.company.account.repository;

import com.company.account.entity.User;
import com.company.account.entity.User.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByNickname(String nickname);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    List<User> findByStatus(UserStatus status);

    Optional<User> findByUserIdAndStatus(Long userId, UserStatus status);

    // 페이지네이션
    Page<User> findAll(Pageable pageable);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    // 검색 기능 (이름, 이메일, 닉네임)
    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    // 검색 + 상태별 필터
    @Query("SELECT u FROM User u WHERE " +
            "u.status = :status AND " +
            "(:keyword IS NULL OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchUsersByStatus(@Param("keyword") String keyword,
                                     @Param("status") UserStatus status,
                                     Pageable pageable);
}
