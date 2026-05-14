package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuarioId;
    private String accion;
    private String recursoId;
    private String recursoTipo;
    private String detalles;
    private LocalDateTime timestamp;
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
