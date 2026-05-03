package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);
}