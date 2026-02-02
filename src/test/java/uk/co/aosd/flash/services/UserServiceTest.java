package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.co.aosd.flash.domain.User;
import uk.co.aosd.flash.domain.UserRole;
import uk.co.aosd.flash.dto.ChangePasswordDto;
import uk.co.aosd.flash.dto.UpdateProfileDto;
import uk.co.aosd.flash.dto.UserDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.InvalidCurrentPasswordException;
import uk.co.aosd.flash.repository.UserRepository;

/**
 * Unit tests for UserService (updateProfile, changePassword).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private UserService userService;

    private UUID userId;
    private User user;
    private OffsetDateTime createdAt;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtTokenProvider);
        userId = UUID.randomUUID();
        createdAt = OffsetDateTime.now();
        user = new User();
        user.setId(userId);
        user.setUsername("johndoe");
        user.setEmail("john@example.com");
        user.setPassword("$2a$12$hashed");
        user.setRoles(UserRole.USER);
        user.setCreatedAt(createdAt);
    }

    @Test
    void updateProfile_success_updatesUsernameAndEmail() {
        final UpdateProfileDto dto = new UpdateProfileDto("newuser", "new@example.com", "CurrentPass123!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPass123!", user.getPassword())).thenReturn(true);
        when(userRepository.existsByUsernameAndIdNot(userId, "newuser")).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot(userId, "new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            final User u = inv.getArgument(0);
            u.setUsername("newuser");
            u.setEmail("new@example.com");
            return u;
        });

        final UserDto result = userService.updateProfile(userId, dto);

        assertEquals(userId, result.id());
        assertEquals("newuser", result.username());
        assertEquals("new@example.com", result.email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_wrongCurrentPassword_throwsInvalidCurrentPasswordException() {
        final UpdateProfileDto dto = new UpdateProfileDto("newuser", "new@example.com", "WrongPass123!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass123!", user.getPassword())).thenReturn(false);

        assertThrows(InvalidCurrentPasswordException.class, () -> userService.updateProfile(userId, dto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_duplicateUsername_throwsDuplicateEntityException() {
        final UpdateProfileDto dto = new UpdateProfileDto("existinguser", "new@example.com", "CurrentPass123!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPass123!", user.getPassword())).thenReturn(true);
        when(userRepository.existsByUsernameAndIdNot(userId, "existinguser")).thenReturn(true);

        final DuplicateEntityException ex = assertThrows(DuplicateEntityException.class,
            () -> userService.updateProfile(userId, dto));
        assertEquals("username", ex.getId());
        assertEquals("existinguser", ex.getName());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_duplicateEmail_throwsDuplicateEntityException() {
        final UpdateProfileDto dto = new UpdateProfileDto("newuser", "existing@example.com", "CurrentPass123!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPass123!", user.getPassword())).thenReturn(true);
        when(userRepository.existsByUsernameAndIdNot(userId, "newuser")).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot(userId, "existing@example.com")).thenReturn(true);

        final DuplicateEntityException ex = assertThrows(DuplicateEntityException.class,
            () -> userService.updateProfile(userId, dto));
        assertEquals("email", ex.getId());
        assertEquals("existing@example.com", ex.getName());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_unchangedUsernameAndEmail_doesNotCheckUniqueness() {
        final UpdateProfileDto dto = new UpdateProfileDto("johndoe", "john@example.com", "CurrentPass123!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPass123!", user.getPassword())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        final UserDto result = userService.updateProfile(userId, dto);

        assertEquals("johndoe", result.username());
        assertEquals("john@example.com", result.email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_usernameTooShort_throwsIllegalArgumentException() {
        final UpdateProfileDto dto = new UpdateProfileDto("ab", "john@example.com", "CurrentPass123!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPass123!", user.getPassword())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(userId, dto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_success_updatesPassword() {
        final ChangePasswordDto dto = new ChangePasswordDto("CurrentPass123!", "NewSecurePass456!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPass123!", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewSecurePass456!")).thenReturn("$2a$12$newHashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.changePassword(userId, dto);

        verify(passwordEncoder).encode("NewSecurePass456!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsInvalidCurrentPasswordException() {
        final ChangePasswordDto dto = new ChangePasswordDto("WrongPass123!", "NewSecurePass456!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass123!", user.getPassword())).thenReturn(false);

        assertThrows(InvalidCurrentPasswordException.class, () -> userService.changePassword(userId, dto));
        verify(passwordEncoder, never()).encode(any(String.class));
        verify(userRepository, never()).save(any(User.class));
    }
}
