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

    private String usuarioId;      // ID del Supervisor que realizó la acción
    private String accion;         // "APROBAR_IMAGEN", "RECHAZAR_IMAGEN", etc.
    private String recursoId;      // ID de la imagen o álbum
    private String recursoTipo;    // "IMAGE", "ALBUM"
    private String detalles;       // Descripción adicional
    private LocalDateTime timestamp;
    private String ipAddress;      // IP desde la que se realizó la acción

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
