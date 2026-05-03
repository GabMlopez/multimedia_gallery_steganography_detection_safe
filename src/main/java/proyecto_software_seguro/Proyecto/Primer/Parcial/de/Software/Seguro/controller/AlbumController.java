package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Album;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Image;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.ImageStatus;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AlbumRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.ImageRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.service.FileStorageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private FileStorageService fileService;

    @Autowired
    private ImageRepository imageRepository;


    @PostMapping("/solicitar")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> solicitarAlbum(@Valid @RequestBody Album album) {
        // El estado inicial debe ser siempre "Pendiente"
        album.setAprobado(false);

        albumRepository.save(album);
        return ResponseEntity.ok("Solicitud de álbum enviada para revisión");
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public List<Album> verPendientes() {
        return albumRepository.findByAprobado(false);
    }

    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<?> aprobarAlbum(@PathVariable Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Álbum no encontrado"));
        album.setAprobado(true);
        albumRepository.save(album);
        return ResponseEntity.ok("Álbum aprobado exitosamente");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SUPERVISOR')")
    public ResponseEntity<?> obtenerDetalleAlbum(@PathVariable Long id) {
        try {
            Album album = albumRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Álbum no encontrado"));

            // Si eres un usuario normal y el álbum no está aprobado, bloqueamos el acceso
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isUser = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER"));

            if (isUser && !album.isAprobado()) {
                return ResponseEntity.status(403).body("Este álbum aún se encuentra en revisión perimetral.");
            }

            // Buscamos SOLO las imágenes que pasaron el filtro de seguridad (CLEAN)
            // Esto evita exponer malware o fotos en cuarentena a los usuarios
            List<Image> imagenesLimpias = imageRepository.findByAlbumIdAndEstado(id, ImageStatus.CLEAN);

            // Empaquetamos la respuesta para React
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("album", album);
            respuesta.put("imagenes", imagenesLimpias);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            return ResponseEntity.status(404).body("Error al obtener el álbum: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<?> rechazarAlbum(@PathVariable Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Álbum no encontrado"));
        albumRepository.delete(album);
        return ResponseEntity.ok("Álbum rechazado y eliminado");
    }

    @GetMapping("/todos")
    @PreAuthorize("hasAnyRole('USER', 'SUPERVISOR')")
    public ResponseEntity<List<Album>> obtenerTodosLosAlbums() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Si es SUPERVISOR, ve aprobados y pendientes
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERVISOR"))) {
            return ResponseEntity.ok(albumRepository.findAll());
        }

        // Si es USER, solo ve los aprobados (Seguridad Perimetral)
        return ResponseEntity.ok(albumRepository.findByAprobado(true));
    }

    @GetMapping("/publico/todos")
    public ResponseEntity<List<Album>> obtenerTodosAnonimo() {
        return ResponseEntity.ok(albumRepository.findByAprobado(true));
    }

    // --- Nuevo endpoint para el detalle del álbum (Público) ---
    @GetMapping("/publico/{id}")
    public ResponseEntity<?> obtenerDetalleAnonimo(@PathVariable Long id) {
        // 1. Buscamos el álbum
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Álbum no encontrado"));

        // 2. Seguridad Perimetral: Si el álbum no está aprobado, un anónimo no debe verlo
        if (!album.isAprobado()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Este álbum aún no ha sido aprobado.");
        }

        // 3. Buscamos solo las imágenes limpias de este álbum
        List<Image> imagenes = imageRepository.findByAlbumIdAndEstado(id, ImageStatus.CLEAN);

        // 4. Devolvemos un objeto con ambas cosas
        Map<String, Object> response = new HashMap<>();
        response.put("album", album);
        response.put("imagenes", imagenes);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/solicitar-lote", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> solicitarAlbumConArchivos(
            @RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "archivos", required = false) MultipartFile[] archivos) {


        try {
            // 1. Guardar la metadata del álbum (Siempre entra como Pendiente)
            Album album = new Album();
            album.setTitulo(titulo);
            album.setDescripcion(descripcion);
            album.setAprobado(false);
            Album albumGuardado = albumRepository.save(album);

            // 2. Procesar el lote de archivos bajo los estándares de seguridad
            if (archivos != null && archivos.length > 0) {
                for (MultipartFile archivo : archivos) {

                    String report = fileService.getSteganographyReport(archivo);
                    // Si el archivo no es válido (Magic Numbers), lo saltamos o podríamos lanzar error
                    if (!fileService.isValidImage(archivo)) {
                        continue;
                    }

                    // Análisis de Esteganografía
                    boolean isSuspicious = fileService.detectSteganography(archivo);

                    if (isSuspicious) {
                        fileService.saveToQuarantine(archivo, albumGuardado.getId());
                    } else {
                        // Incluso si es "Clean", la imagen se asocia a un álbum que está "Pendiente"
                        // por lo que el usuario público no la verá hasta que el álbum sea aprobado.
                        fileService.saveCleanImage(archivo, albumGuardado.getId());
                    }

                    if (report != null) {
                        // Si hay reporte, es sospechosa. Guardamos el porqué en 'motivoAlerta'
                        fileService.saveToQuarantine(archivo, albumGuardado.getId());
                    } else {
                        fileService.saveCleanImage(archivo, albumGuardado.getId());
                    }
                }
            }

            return ResponseEntity.ok("Álbum y archivos enviados a revisión perimetral exitosamente.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error interno al procesar el lote multimedia.");
        }
    }
}