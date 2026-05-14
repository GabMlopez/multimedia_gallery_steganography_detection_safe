package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.dto.LoginRequestDTO;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.dto.UserRegistrationDTO;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Role;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.User;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.service.LoginAttemptService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            return ResponseEntity.badRequest().body("Error: El nombre de usuario ya está en uso.");
        }

        User user = new User();
        user.setUsername(dto.getUsername());

        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        user.setRole(dto.getRole());

        userRepository.save(user);

        return ResponseEntity.ok("Usuario registrado exitosamente bajo estándares de Software Seguro.");
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest, HttpServletRequest request) {
        String username = (loginRequest.getUsername() != null) ? loginRequest.getUsername().trim() : "";
        String password = (loginRequest.getPassword() != null) ? loginRequest.getPassword().trim() : "";

        if (loginAttemptService.isBlocked(username)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Cuenta bloqueada temporalmente por múltiples intentos fallidos. Intente de nuevo en unos minutos.");
        }

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new BadCredentialsException("Usuario o contraseña incorrectos");
            }

            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority(user.getRole().name())
            );

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);

            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            loginAttemptService.loginSucceeded(username);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Login exitoso");
            response.put("username", user.getUsername());
            response.put("role", user.getRole().name());

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario o contraseña incorrectos");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno en el servidor");
        }
    }

    /**
     * Obtiene la dirección IP del cliente
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "UNKNOWN";

            HttpServletRequest request = attrs.getRequest();
            String xForwarded = request.getHeader("X-Forwarded-For");
            if (xForwarded != null && !xForwarded.isEmpty()) {
                return xForwarded.split(",")[0];
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    @GetMapping("/csrf")
    public void getCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrfToken != null) {
            String tokenValue = csrfToken.getToken();
            System.out.println("CSRF Token generado con éxito: " + tokenValue);
        }
    }
}
