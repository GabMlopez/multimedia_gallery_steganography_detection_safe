package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Album;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Image;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.ImageStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class ImageRepositoryTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private AlbumRepository albumRepository;

    private Album createTestAlbum() {
        Album album = new Album();
        album.setTitulo("Album Test");
        album.setDescripcion("Descripcion test");
        return albumRepository.save(album);
    }

    private Image createTestImage(String nombre, ImageStatus status, Album album, String hash) {
        Image img = new Image();
        img.setNombreArchivo(nombre);
        img.setRutaLocal("uploads/" + (status == ImageStatus.QUARANTINE ? "quarantine" : "safe") + "/" + nombre);
        img.setEstado(status);
        img.setAlbum(album);
        img.setHash(hash);
        return imageRepository.save(img);
    }

    @Test
    @DisplayName("findByEstado - filtrar por QUARANTINE retorna solo imagenes en cuarentena")
    void findByEstado_quarantine_returnsOnlyQuarantineImages() {
        Album album = createTestAlbum();
        createTestImage("clean.jpg", ImageStatus.CLEAN, album, "hash1");
        createTestImage("SUSPECT_malware.jpg", ImageStatus.QUARANTINE, album, "hash2");
        createTestImage("SUSPECT_stego.jpg", ImageStatus.QUARANTINE, album, "hash3");

        List<Image> quarantined = imageRepository.findByEstado(ImageStatus.QUARANTINE);

        assertEquals(2, quarantined.size());
        assertTrue(quarantined.stream().allMatch(img -> img.getEstado() == ImageStatus.QUARANTINE));
    }

    @Test
    @DisplayName("findByAlbumIdAndEstado - filtrar CLEAN por album retorna solo las limpias")
    void findByAlbumIdAndEstado_clean_returnsOnlyCleanImages() {
        Album album = createTestAlbum();
        createTestImage("img1.jpg", ImageStatus.CLEAN, album, "hash1");
        createTestImage("img2.jpg", ImageStatus.CLEAN, album, "hash2");
        createTestImage("SUSPECT_bad.jpg", ImageStatus.QUARANTINE, album, "hash3");

        List<Image> cleanImages = imageRepository.findByAlbumIdAndEstado(album.getId(), ImageStatus.CLEAN);

        assertEquals(2, cleanImages.size());
        assertTrue(cleanImages.stream().allMatch(img -> img.getEstado() == ImageStatus.CLEAN));
    }

    @Test
    @DisplayName("countByEstado - contar QUARANTINE retorna el numero correcto")
    void countByEstado_quarantine_returnsCorrectCount() {
        Album album = createTestAlbum();
        createTestImage("clean.jpg", ImageStatus.CLEAN, album, "hash1");
        createTestImage("SUSPECT_1.jpg", ImageStatus.QUARANTINE, album, "hash2");
        createTestImage("SUSPECT_2.jpg", ImageStatus.QUARANTINE, album, "hash3");

        long count = imageRepository.countByEstado(ImageStatus.QUARANTINE);

        assertEquals(2L, count);
    }

    @Test
    @DisplayName("existsByAlbumIdAndHash - duplicado detectado correctamente")
    void existsByAlbumIdAndHash_duplicate_returnsTrue() {
        Album album = createTestAlbum();
        createTestImage("original.jpg", ImageStatus.CLEAN, album, "hash_abc_123");

        boolean exists = imageRepository.existsByAlbumIdAndHash(album.getId(), "hash_abc_123");

        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByAlbumIdAndHash - hash no existente retorna false")
    void existsByAlbumIdAndHash_notFound_returnsFalse() {
        Album album = createTestAlbum();
        createTestImage("original.jpg", ImageStatus.CLEAN, album, "hash_abc_123");

        boolean exists = imageRepository.existsByAlbumIdAndHash(album.getId(), "hash_xyz_999");

        assertFalse(exists);
    }

    @Test
    @DisplayName("findByAlbumId - retorna todas las imagenes de un album")
    void findByAlbumId_returnsAllImagesInAlbum() {
        Album album = createTestAlbum();
        createTestImage("img1.jpg", ImageStatus.CLEAN, album, "hash1");
        createTestImage("img2.jpg", ImageStatus.CLEAN, album, "hash2");

        List<Image> images = imageRepository.findByAlbumId(album.getId());

        assertEquals(2, images.size());
    }

    @Test
    @DisplayName("estado REJECTED se maneja correctamente")
    void rejectedStatus_worksCorrectly() {
        Album album = createTestAlbum();
        Image img = createTestImage("rejected.jpg", ImageStatus.REJECTED, album, "hash_rej");

        Image found = imageRepository.findById(img.getId()).orElseThrow();
        assertEquals(ImageStatus.REJECTED, found.getEstado());
    }
}
