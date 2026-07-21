package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AlbumRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CP37 | Re-sanitizacion al actualizar la descripcion de un album
 * (SQAP seccion 3.2, pag. 33).
 *
 * El SQAP plantea este caso como una actualizacion via API, pero AlbumController no
 * expone ningun endpoint para editar la descripcion de un album existente. El control
 * real es el callback @PreUpdate de la entidad, asi que la prueba se ejecuta en la capa
 * de persistencia, que es donde el callback efectivamente se dispara.
 */
@DataJpaTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class AlbumSanitizationJpaTest {

    @Autowired
    private AlbumRepository albumRepository;

    private Album nuevoAlbum(String titulo, String descripcion) {
        Album album = new Album();
        album.setTitulo(titulo);
        album.setDescripcion(descripcion);
        return album;
    }

    // ------------------------------------------------------------------
    // CP37 - @PreUpdate vuelve a sanitizar en cada actualizacion
    // ------------------------------------------------------------------
    @Test
    @DisplayName("CP37 - Actualizar la descripcion con un payload onerror la deja neutralizada")
    void actualizarDescripcion_conPayloadOnerror_seGuardaEscapada() {
        Album album = albumRepository.saveAndFlush(
                nuevoAlbum("Album de prueba", "Descripcion inicial limpia"));
        Long id = album.getId();

        assertThat(album.getDescripcion()).isEqualTo("Descripcion inicial limpia");

        // Actualizacion con payload XSS: dispara @PreUpdate.
        album.setDescripcion("<img src=x onerror=alert(1)>");
        albumRepository.saveAndFlush(album);

        Album recuperado = albumRepository.findById(id).orElseThrow();

        // La etiqueta quedo escapada: el navegador la renderiza como texto, no como
        // un elemento con atributo onerror ejecutable.
        assertThat(recuperado.getDescripcion()).doesNotContain("<img");
        assertThat(recuperado.getDescripcion()).contains("&lt;img");
        assertThat(recuperado.getDescripcion()).doesNotContain("<");
    }

    // ------------------------------------------------------------------
    // CAL-01 - Efecto secundario: doble escapado en cada UPDATE
    // ------------------------------------------------------------------
    /**
     * Evidencia del defecto de calidad CAL-01. XssSanitizer.escapeHtml() se aplica en
     * @PrePersist y otra vez en cada @PreUpdate, sin comprobar si el valor ya venia
     * escapado. Cada actualizacion del album vuelve a escapar los ampersands, de modo
     * que "&lt;" se convierte en "&amp;lt;" y el texto se degrada de forma acumulativa.
     *
     * No es una vulnerabilidad (el resultado es mas inerte, no menos), pero corrompe el
     * contenido mostrado al usuario. Severidad baja.
     */
    @Test
    @DisplayName("CAL-01 - Una actualizacion que no toca la descripcion la re-escapa igual")
    void actualizarOtroCampo_reEscapaLaDescripcionYaEscapada() {
        Album album = albumRepository.saveAndFlush(
                nuevoAlbum("Titulo original", "<b>hola</b>"));
        Long id = album.getId();

        String trasInsert = albumRepository.findById(id).orElseThrow().getDescripcion();
        assertThat(trasInsert).isEqualTo("&lt;b&gt;hola&lt;/b&gt;");

        // Se actualiza SOLO el titulo; la descripcion no se toca.
        album.setTitulo("Titulo editado");
        albumRepository.saveAndFlush(album);

        String trasUpdate = albumRepository.findById(id).orElseThrow().getDescripcion();

        // Aun asi, @PreUpdate la vuelve a escapar: los & se convierten en &amp;
        assertThat(trasUpdate).isEqualTo("&amp;lt;b&amp;gt;hola&amp;lt;/b&amp;gt;");
        assertThat(trasUpdate).isNotEqualTo(trasInsert);
    }
}
