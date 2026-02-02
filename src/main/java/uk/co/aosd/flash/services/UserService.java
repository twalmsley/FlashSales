package uk.co.aosd.flash.services;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import uk.co.aosd.flash.domain.User;
import uk.co.aosd.flash.domain.UserRole;
import uk.co.aosd.flash.dto.AuthResponseDto;
import uk.co.aosd.flash.dto.ChangePasswordDto;
import uk.co.aosd.flash.dto.LoginDto;
import uk.co.aosd.flash.dto.RegisterDto;
import uk.co.aosd.flash.dto.UpdateProfileDto;
import uk.co.aosd.flash.dto.UserDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.InvalidCurrentPasswordException;
import uk.co.aosd.flash.repository.UserRepository;

/**
 * Service for user management and authentication.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user.
     *
     * @param registerDto registration data
     * @return authentication response with JWT token
     * @throws DuplicateEntityException if username or email already exists
     */
    @Transactional
    public AuthResponseDto register(final RegisterDto registerDto) {
        log.info("Registering new user: {}", registerDto.username());

        // Check if username already exists
        if (userRepository.existsByUsername(registerDto.username())) {
            throw new DuplicateEntityException(registerDto.username(), "username");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerDto.email())) {
            throw new DuplicateEntityException(registerDto.email(), "email");
        }

        // Create new user
        final User user = new User();
        user.setUsername(registerDto.username());
        user.setEmail(registerDto.email());
        user.setPassword(passwordEncoder.encode(registerDto.password()));
        user.setRoles(UserRole.USER);

        final User savedUser = userRepository.save(user);
        log.info("User registered successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());

        // Generate JWT token
        final String token = jwtTokenProvider.generateToken(
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getRoles()
        );

        return new AuthResponseDto(
            token,
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getEmail(),
            savedUser.getRoles()
        );
    }

    /**
     * Authenticate a user and return JWT token.
     *
     * @param loginDto login credentials
     * @return authentication response with JWT token
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     */
    @Transactional(readOnly = true)
    public AuthResponseDto authenticate(final LoginDto loginDto) {
        log.info("Authenticating user: {}", loginDto.username());

        // Find user by username or email
        final User user = userRepository.findByUsername(loginDto.username())
            .or(() -> userRepository.findByEmail(loginDto.username()))
            .orElseThrow(() -> {
                log.warn("User not found: {}", loginDto.username());
                return new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
            });

        // Verify password
        if (!passwordEncoder.matches(loginDto.password(), user.getPassword())) {
            log.warn("Invalid password for user: {}", loginDto.username());
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
        }

        log.info("User authenticated successfully: {} (ID: {})", user.getUsername(), user.getId());

        // Generate JWT token
        final String token = jwtTokenProvider.generateToken(
            user.getId(),
            user.getUsername(),
            user.getRoles()
        );

        return new AuthResponseDto(
            token,
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRoles()
        );
    }

    /**
     * Find user by ID.
     *
     * @param userId user ID
     * @return user DTO
     * @throws jakarta.persistence.EntityNotFoundException if user not found
     */
    @Cacheable(value = "users", key = "#userId")
    @Transactional(readOnly = true)
    public UserDto findById(final UUID userId) {
        final User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("User not found: {}", userId);
                return new jakarta.persistence.EntityNotFoundException("User not found: " + userId);
            });

        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRoles(),
            user.getCreatedAt()
        );
    }

    /**
     * Find user by username.
     *
     * @param username username
     * @return user or null if not found
     */
    @Cacheable(value = "users", key = "#username")
    @Transactional(readOnly = true)
    public User findByUsername(final String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Update profile (username and/or email) for the given user. Current password must match.
     *
     * @param userId user ID
     * @param dto    update profile DTO (optional username/email, required currentPassword)
     * @return updated user DTO
     * @throws InvalidCurrentPasswordException if current password is wrong
     * @throws DuplicateEntityException        if new username or email is already taken by another user
     */
    @CacheEvict(value = "users", allEntries = true)
    @Transactional
    public UserDto updateProfile(final UUID userId, final UpdateProfileDto dto) {
        final User user = userRepository.findById(userId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found: " + userId));

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            log.warn("Invalid current password for profile update: userId={}", userId);
            throw new InvalidCurrentPasswordException();
        }

        if (dto.username() != null && !dto.username().isBlank()) {
            final String trimmed = dto.username().trim();
            if (trimmed.length() < 3 || trimmed.length() > 50) {
                throw new IllegalArgumentException("Username must be between 3 and 50 characters");
            }
            if (!trimmed.equals(user.getUsername()) && userRepository.existsByUsernameAndIdNot(trimmed, userId)) {
                throw new DuplicateEntityException("username", trimmed);
            }
            user.setUsername(trimmed);
        }
        if (dto.email() != null && !dto.email().isBlank()) {
            final String trimmed = dto.email().trim();
            if (trimmed.length() > 100) {
                throw new IllegalArgumentException("Email must not exceed 100 characters");
            }
            if (!trimmed.equals(user.getEmail()) && userRepository.existsByEmailAndIdNot(trimmed, userId)) {
                throw new DuplicateEntityException("email", trimmed);
            }
            user.setEmail(trimmed);
        }

        final User saved = userRepository.save(user);
        log.info("Profile updated for user: {} (ID: {})", saved.getUsername(), saved.getId());
        return new UserDto(
            saved.getId(),
            saved.getUsername(),
            saved.getEmail(),
            saved.getRoles(),
            saved.getCreatedAt()
        );
    }

    /**
     * Change password for the given user. Current password must match.
     *
     * @param userId user ID
     * @param dto    change password DTO (currentPassword, newPassword)
     * @throws InvalidCurrentPasswordException if current password is wrong
     */
    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public void changePassword(final UUID userId, final ChangePasswordDto dto) {
        final User user = userRepository.findById(userId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found: " + userId));

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            log.warn("Invalid current password for password change: userId={}", userId);
            throw new InvalidCurrentPasswordException();
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {} (ID: {})", user.getUsername(), userId);
    }
}
