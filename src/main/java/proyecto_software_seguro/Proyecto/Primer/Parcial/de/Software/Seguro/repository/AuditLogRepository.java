package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.AuditLog;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUsuarioId(String usuarioId);
    List<AuditLog> findByRecursoId(String recursoId);
    List<AuditLog> findByAccion(String accion);
}
