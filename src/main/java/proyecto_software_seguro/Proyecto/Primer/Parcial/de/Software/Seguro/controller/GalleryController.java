package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Album;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Image;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.ImageStatus;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AlbumRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.ImageRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/public")
public class GalleryController {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ImageRepository imageRepository;

    @GetMapping("/albums")
    public List<Album> getPublicAlbums() {
        return albumRepository.findByAprobadoTrue();
    }

    @GetMapping("/album/{albumId}/images")
    public List<Image> getAlbumImages(@PathVariable Long albumId) {
        return imageRepository.findByAlbumIdAndEstado(albumId, ImageStatus.CLEAN);
    }

    @GetMapping("/view/{filename}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) throws IOException {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.status(403).body(null);
        }

        Path basePath = Paths.get("uploads/safe").toAbsolutePath();
        Path path = basePath.resolve(filename).normalize();

        if (!path.startsWith(basePath)) {
            return ResponseEntity.status(403).body(null);
        }

        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}
