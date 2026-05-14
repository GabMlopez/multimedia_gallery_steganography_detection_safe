package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.util.XssSanitizer;

@Entity
@Data
@Table(name = "albums")
public class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 100, message = "El título no puede exceder 100 caracteres")
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcion;

    private boolean aprobado = false;

    @ManyToOne
    private User propietario;

    @PrePersist
    @PreUpdate
    public void sanitizeDescription() {
        if (this.descripcion != null) {
            this.descripcion = XssSanitizer.escapeHtml(this.descripcion);
        }
    }
}