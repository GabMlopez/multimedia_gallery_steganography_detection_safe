package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Album;

import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findByAprobado(boolean aprobado);

    List<Album> findByAprobadoTrue();

    List<Album> findByPropietarioId(Long propietarioId);

    List<Album> findByAprobadoFalse();

}