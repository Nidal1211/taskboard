package de.nidal.taskboard.auth;

import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import de.nidal.taskboard.common.ApiException;
import de.nidal.taskboard.security.TokenService;
import de.nidal.taskboard.user.User;
import de.nidal.taskboard.user.UserRepository;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public UserResponse register(RegisterRequest request) {
        String email = request.email();
        String password = request.password();

        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "E-Mail und Passwort sind Pflichtfelder");
        }

        email = email.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bitte gib eine gültige E-Mail-Adresse ein");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwort muss mindestens 8 Zeichen lang sein");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Diese E-Mail ist bereits registriert");
        }

        User user = userRepository.save(new User(email, passwordEncoder.encode(password)));
        return new UserResponse(user.getId(), user.getEmail());
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        String password = request.password() == null ? "" : request.password();

        User user = userRepository.findByEmail(email)
                .filter(found -> passwordEncoder.matches(password, found.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "E-Mail oder Passwort ist falsch"));

        return new LoginResponse(tokenService.createToken(user));
    }
}