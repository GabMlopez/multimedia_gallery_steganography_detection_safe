package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Album;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AlbumRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CP36 y CP38 | Proteccion XSS a nivel de endpoint (SQAP seccion 3.2, pags. 32-33).
 *
 * NO se mockea AlbumRepository: la sanitizacion vive en los callbacks @PrePersist /
 * @PreUpdate de la entidad Album, que solo se ejecutan con persistencia real. Con un
 * repositorio mockeado el callback nunca se dispara y el test daria un falso verde.
 * Por eso se usa la base H2 del perfil "test" y se lee lo que quedo realmente guardado.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class XssSanitizationTest {

    private static final String PAYLOAD_SCRIPT = "<script>alert('XSS')</script>Hola";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        albumRepository.deleteAll();
    }

    /** Fuerza el INSERT/UPDATE y vacia el contexto de persistencia para leer desde la BD. */
    private Album releerDesdeBaseDeDatos() {
        entityManager.flush();
        entityManager.clear();
        List<Album> albums = albumRepository.findAll();
        assertThat(albums).hasSize(1);
        return albums.get(0);
    }

    // ------------------------------------------------------------------
    // CP36 - Sanitizacion de la descripcion al crear un album
    // ------------------------------------------------------------------
    @Test
    @WithMockUser(username = "usuario", roles = "USER")
    @DisplayName("CP36 - POST /api/albums/solicitar escapa el script en la descripcion")
    void solicitarAlbum_conScriptEnDescripcion_seGuardaEscapado() throws Exception {
        String body = """
                {"titulo":"Album de prueba","descripcion":"<script>alert('XSS')</script>Hola"}
                """;

        mockMvc.perform(post("/api/albums/solicitar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Album guardado = releerDesdeBaseDeDatos();

        // El payload quedo inerte: entidades HTML en lugar de etiquetas ejecutables.
        assertThat(guardado.getDescripcion()).doesNotContain("<script>");
        assertThat(guardado.getDescripcion()).doesNotContain("</script>");
        assertThat(guardado.getDescripcion()).contains("&lt;script&gt;");
        assertThat(guardado.getDescripcion()).endsWith("Hola");

        // El album entra siempre como pendiente de revision.
        assertThat(guardado.isAprobado()).isFalse();
    }

    // ------------------------------------------------------------------
    // CP38 - HALLAZGO XSS-01: el titulo NO se sanitiza
    // ------------------------------------------------------------------
    /**
     * Evidencia ejecutable del hallazgo XSS-01. Este test documenta el comportamiento
     * ACTUAL (vulnerable) y por eso pasa en verde: sirve como prueba objetiva para el
     * informe. La comprobacion de la conducta deseada esta en el test @Disabled de abajo.
     */
    @Test
    @WithMockUser(username = "usuario", roles = "USER")
    @DisplayName("CP38 - HALLAZGO XSS-01: el titulo se almacena SIN sanitizar (comportamiento actual)")
    void solicitarAlbumLote_conScriptEnTitulo_seAlmacenaSinSanitizar() throws Exception {
        mockMvc.perform(multipart("/api/albums/solicitar-lote")
                        .param("titulo", "<script>alert(1)</script>")
                        .param("descripcion", "Descripcion valida")
                        .with(csrf()))
                .andExpect(status().isOk());

        Album guardado = releerDesdeBaseDeDatos();

        // La descripcion SI se sanitiza...
        assertThat(guardado.getDescripcion()).isEqualTo("Descripcion valida");

        // ...pero el titulo NO. Album.sanitizeDescription() solo toca 'descripcion'
        // y AlbumController.solicitarAlbumConArchivos() asigna el titulo en crudo.
        assertThat(guardado.getTitulo()).isEqualTo("<script>alert(1)</script>");
        assertThat(guardado.getTitulo()).contains("<script>");
    }

    /**
     * Conducta esperada segun el criterio de aceptacion del SQAP. Permanece deshabilitado
     * hasta que se corrija XSS-01; al arreglarlo, quitar @Disabled y este test pasa a ser
     * la prueba de regresion.
     *
     * Correccion propuesta: extender el callback de Album para escapar tambien el titulo.
     */
    @Test
    @Disabled("HALLAZGO XSS-01: el titulo del album no se sanitiza. Habilitar tras la correccion.")
    @WithMockUser(username = "usuario", roles = "USER")
    @DisplayName("CP38 - Conducta esperada: el titulo deberia escaparse igual que la descripcion")
    void solicitarAlbumLote_conScriptEnTitulo_deberiaSanitizarse() throws Exception {
        mockMvc.perform(multipart("/api/albums/solicitar-lote")
                        .param("titulo", "<script>alert(1)</script>")
                        .param("descripcion", "Descripcion valida")
                        .with(csrf()))
                .andExpect(status().isOk());

        Album guardado = releerDesdeBaseDeDatos();

        assertThat(guardado.getTitulo()).doesNotContain("<script>");
        assertThat(guardado.getTitulo()).contains("&lt;script&gt;");
    }
}
