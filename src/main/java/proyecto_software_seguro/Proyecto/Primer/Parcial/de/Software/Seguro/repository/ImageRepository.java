package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Image;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.ImageStatus;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByEstado(ImageStatus estado);

    List<Image> findByAlbumIdAndEstado(Long albumId, ImageStatus estado);
    List<Image> findByAlbumId(Long albumId);
    long countByEstado(ImageStatus estado);
    boolean existsByAlbumIdAndHash(Long albumId, String hash);
}