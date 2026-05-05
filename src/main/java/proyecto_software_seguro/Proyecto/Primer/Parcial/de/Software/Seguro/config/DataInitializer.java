package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Role;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.User;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedDefaultUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("user@test.com")) {
                User user = new User();
                user.setUsername("user@test.com");
                user.setPassword(passwordEncoder.encode("User123!Pass"));
                user.setRole(Role.ROLE_USER);
                userRepository.save(user);
            }

            if (!userRepository.existsByUsername("supervisor@test.com")) {
                User supervisor = new User();
                supervisor.setUsername("supervisor@test.com");
                supervisor.setPassword(passwordEncoder.encode("Supervisor123!"));
                supervisor.setRole(Role.ROLE_SUPERVISOR);
                userRepository.save(supervisor);
            }
        };
    }
}