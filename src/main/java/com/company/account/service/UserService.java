package com.company.account.service;

import com.company.account.config.CacheConfig;
import com.company.account.dto.UserRequest;
import com.company.account.dto.UserResponse;
import com.company.account.entity.User;
import com.company.account.entity.User.UserStatus;
import com.company.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final CacheInvalidationService cacheInvalidationService;

    @Transactional
    public UserResponse createUser(UserRequest.Create request) {
        log.info("Creating new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + request.getEmail());
        }

        if (request.getNickname() != null && userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다: " + request.getNickname());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .profileImageUrl(request.getProfileImageUrl())
                .provider(request.getProvider())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getUserId());

        return UserResponse.fromEntity(savedUser);
    }

    @Cacheable(value = CacheConfig.CACHE_USER, key = "#userId")
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        log.info("Fetching user with ID: {} (will cache if not exists)", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        return UserResponse.fromEntity(user);
    }

    @Cacheable(value = CacheConfig.CACHE_USER_EMAIL, key = "#email")
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.info("Fetching user with email: {} (will cache if not exists)", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");

        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByStatus(UserStatus status) {
        log.info("Fetching users with status: {}", status);

        return userRepository.findByStatus(status).stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserRequest.Update request) {
        log.info("Updating user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getNickname() != null) {
            if (userRepository.existsByNickname(request.getNickname()) &&
                    !request.getNickname().equals(user.getNickname())) {
                throw new IllegalArgumentException("이미 존재하는 닉네임입니다: " + request.getNickname());
            }
            user.setNickname(request.getNickname());
        }

        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getPassword() != null) {
            if (request.getPassword().length() < 8) {
                throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", userId);

        // Invalidate user info cache
        cacheInvalidationService.invalidateUserInfoCache(userId);

        return UserResponse.fromEntity(updatedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Soft deleting user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("User soft deleted successfully: {}", userId);

        // Invalidate all user-related caches
        cacheInvalidationService.invalidateUserCaches(userId);
    }

    @Transactional
    public void hardDeleteUser(Long userId) {
        log.info("Hard deleting user with ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
        }

        userRepository.deleteById(userId);
        log.info("User hard deleted successfully: {}", userId);

        // Invalidate all user-related caches
        cacheInvalidationService.invalidateUserCaches(userId);
    }

    @Transactional
    public UserResponse updateUserRole(Long userId, UserRequest.UpdateRole request) {
        log.info("Updating role for user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        user.setRole(request.getRole());

        User updatedUser = userRepository.save(user);
        log.info("User role updated successfully: {}", userId);

        // Invalidate user info cache (role affects access control)
        cacheInvalidationService.invalidateUserInfoCache(userId);

        return UserResponse.fromEntity(updatedUser);
    }

    @Transactional
    public UserResponse suspendUser(Long userId) {
        log.info("Suspending user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        user.setStatus(UserStatus.SUSPENDED);

        User updatedUser = userRepository.save(user);
        log.info("User suspended successfully: {}", userId);

        // Invalidate all user-related caches (suspended user should not access)
        cacheInvalidationService.invalidateUserCaches(userId);

        return UserResponse.fromEntity(updatedUser);
    }

    @Transactional
    public UserResponse activateUser(Long userId) {
        log.info("Activating user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        user.setStatus(UserStatus.ACTIVE);
        // 재활성화 시 deletedAt 초기화
        if (user.getDeletedAt() != null) {
            user.setDeletedAt(null);
        }

        User updatedUser = userRepository.save(user);
        log.info("User activated successfully: {}", userId);

        // Invalidate user info cache (status changed)
        cacheInvalidationService.invalidateUserInfoCache(userId);

        return UserResponse.fromEntity(updatedUser);
    }

    @Transactional
    public void changePassword(Long userId, UserRequest.ChangePassword request) {
        log.info("Changing password for user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 소셜 로그인 사용자인지 확인
        boolean isSocialUser = user.getProvider() != null;

        if (isSocialUser) {
            // 소셜 로그인 사용자: 기존 비밀번호 확인 없이 새 비밀번호만 설정
            log.info("Social user detected. Setting new password without old password verification");
        } else {
            // 일반 사용자: 현재 비밀번호 확인 필요
            if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("현재 비밀번호를 입력해주세요");
            }
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다");
            }
        }

        // 새 비밀번호 설정
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userId);

        // Invalidate refresh token cache (force re-login for security)
        cacheInvalidationService.invalidateRefreshTokenCache(userId);
    }

    /**
     * 비밀번호 설정 (소셜 로그인 사용자 전용)
     * 기존 비밀번호 확인 없이 새 비밀번호만 설정
     */
    @Transactional
    public void setPassword(Long userId, UserRequest.SetPassword request) {
        log.info("Setting password for user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 소셜 로그인 사용자인지 확인
        if (user.getProvider() == null) {
            throw new IllegalArgumentException("일반 계정은 비밀번호 설정 기능을 사용할 수 없습니다. 비밀번호 변경 기능을 사용해주세요");
        }

        // 새 비밀번호 설정
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password set successfully for social user: {}", userId);

        // Invalidate refresh token cache (force re-login for security)
        cacheInvalidationService.invalidateRefreshTokenCache(userId);
    }

    @Transactional(readOnly = true)
    public void verifyPassword(Long userId, UserRequest.VerifyPassword request) {
        log.info("Verifying password for user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        log.info("Password verified successfully for user: {}", userId);
    }

    /**
     * 페이지네이션과 검색을 지원하는 사용자 조회
     * @param keyword 검색어 (이름, 이메일, 닉네임)
     * @param status 상태 필터 (null이면 전체)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param sortBy 정렬 기준 (기본: createdAt)
     * @param sortDirection 정렬 방향 (ASC/DESC, 기본: DESC)
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsersWithPagination(
            String keyword,
            UserStatus status,
            int page,
            int size,
            String sortBy,
            String sortDirection) {

        log.info("Searching users with keyword: {}, status: {}, page: {}, size: {}",
                keyword, status, page, size);

        Sort sort = Sort.by(
                sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage;

        if (keyword == null || keyword.trim().isEmpty()) {
            // 검색어 없음
            if (status == null) {
                userPage = userRepository.findAllUsersOnly(User.UserRole.ADMIN, pageable);
            } else {
                userPage = userRepository.findByStatusUsersOnly(status, User.UserRole.ADMIN, pageable);
            }
        } else {
            // 검색어 있음
            if (status == null) {
                userPage = userRepository.searchUsersOnly(keyword, User.UserRole.ADMIN, pageable);
            } else {
                userPage = userRepository.searchUsersByStatusUsersOnly(keyword, status, User.UserRole.ADMIN, pageable);
            }
        }

        return userPage.map(UserResponse::fromEntity);
    }
}
