package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreArchivo;
    private String rutaLocal;

    @Enumerated(EnumType.STRING)
    private ImageStatus estado;

    private String motivoAlerta;

    @ManyToOne
    private Album album;
}