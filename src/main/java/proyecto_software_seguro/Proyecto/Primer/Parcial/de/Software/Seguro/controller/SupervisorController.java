package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Album;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Image;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.ImageStatus;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AlbumRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.ImageRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AuditLogRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.service.FileStorageService;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.service.AuditService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class SupervisorController {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private FileStorageService fileService;

    @Autowired
    private AuditService auditService;


    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<?> obtenerSolicitudesPendientes() {
        List<Album> pendientes = albumRepository.findByAprobadoFalse();
        List<Map<String, Object>> respuesta = new ArrayList<>();

        for (Album album : pendientes) {
            Map<String, Object> albumData = new HashMap<>();
            albumData.put("id", album.getId());
            albumData.put("titulo", album.getTitulo());
            albumData.put("descripcion", album.getDescripcion());

            List<Image> imagenes = imageRepository.findByAlbumId(album.getId());
            albumData.put("imagenes", imagenes);

            respuesta.add(albumData);
        }

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/albums/{id}/aprobar")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<?> aprobarAlbum(@PathVariable Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Álbum no encontrado"));

        album.setAprobado(true);
        albumRepository.save(album);

        String supervisorId = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.logAction(supervisorId, "APROBAR_ALBUM", id.toString(), "ALBUM", "Aprobó el álbum: " + album.getTitulo());

        return ResponseEntity.ok("Álbum aprobado y publicado exitosamente.");
    }

    @DeleteMapping("/albums/{id}/rechazar")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<?> rechazarAlbum(@PathVariable Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Álbum no encontrado"));

        List<Image> imagenes = imageRepository.findByAlbumId(id);
        for (Image img : imagenes) {
            try {
                Files.deleteIfExists(Paths.get(img.getRutaLocal()));
            } catch (IOException e) {
                System.out.println("No se pudo borrar el archivo físico: " + img.getRutaLocal());
            }
        }

        imageRepository.deleteAll(imagenes);
        albumRepository.delete(album);

        String supervisorId = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.logAction(supervisorId, "RECHAZAR_ALBUM", id.toString(), "ALBUM", "Rechazó y destruyó el álbum: " + album.getTitulo());

        return ResponseEntity.ok("Álbum y evidencias destruidas permanentemente.");
    }


    @GetMapping("/quarantine")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public List<Image> getQuarantineGallery() {
        return imageRepository.findByEstado(ImageStatus.QUARANTINE);
    }

    @GetMapping("/quarantine/view/{filename}")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<Resource> serveQuarantinedImage(@PathVariable String filename) {
        try {
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.status(403).build();
            }

            Path path = Paths.get("uploads/quarantine").resolve(filename).normalize();
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/image/{id}/approve")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<?> approveImage(@PathVariable Long id) {
        try {
            Image image = imageRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Imagen no encontrada"));

            fileService.approveQuarantinedImage(id);

            String supervisorId = SecurityContextHolder.getContext().getAuthentication().getName();
            auditService.logAction(supervisorId, "APROBAR_IMAGEN", id.toString(), "IMAGE", "Aprobó imagen: " + image.getNombreArchivo());

            return ResponseEntity.ok("Imagen aprobada y enviada a la bóveda segura.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Imagen no encontrada");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/image/{id}/reject")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<?> rejectImage(@PathVariable Long id) {
        try {
            Image image = imageRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Imagen no encontrada"));

            fileService.rejectQuarantinedImage(id);

            String supervisorId = SecurityContextHolder.getContext().getAuthentication().getName();
            auditService.logAction(supervisorId, "RECHAZAR_IMAGEN", id.toString(), "IMAGE", "Rechazó imagen: " + image.getNombreArchivo());

            return ResponseEntity.ok("Imagen rechazada y eliminada de los servidores.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Imagen no encontrada");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/audit-log")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<?> getAuditLog() {
        try {
            return ResponseEntity.ok(auditLogRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al acceder a logs de auditoría");
        }
    }
}