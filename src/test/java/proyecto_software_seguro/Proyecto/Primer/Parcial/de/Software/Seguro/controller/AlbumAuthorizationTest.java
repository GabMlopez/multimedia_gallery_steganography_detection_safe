package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Album;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AlbumRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-01 | Frontera de autorizacion de AlbumController (hallazgo emergente de la
 * auditoria; NO forma parte de los 10 casos del SQAP).
 *
 * Hipotesis: el proyecto no declara @EnableMethodSecurity, por lo que las anotaciones
 * @PreAuthorize de los controladores no se evaluan. El filter chain de SecurityConfig
 * cubre /api/admin/** y POST /api/albums/**, pero los verbos PUT y DELETE sobre
 * /api/albums/** caen en .anyRequest().authenticated(), que solo exige estar logueado.
 *
 * Estos tests documentan el comportamiento REAL medido. Los que describen la conducta
 * deseada quedan @Disabled hasta que se corrija el hallazgo.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AlbumAuthorizationTest {

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

    private Album albumPendiente() {
        Album album = new Album();
        album.setTitulo("Album pendiente de revision");
        album.setDescripcion("Contenido en espera de aprobacion del supervisor");
        album.setAprobado(false);
        return albumRepository.saveAndFlush(album);
    }

    // ------------------------------------------------------------------
    // SEC-01.a - Un rol USER aprueba un album (deberia ser exclusivo del SUPERVISOR)
    // ------------------------------------------------------------------
    @Test
    @WithMockUser(username = "usuario_comun", roles = "USER")
    @DisplayName("SEC-01.a - PUT /api/albums/{id}/aprobar con rol USER: comportamiento actual")
    void aprobarAlbum_conRolUser_comportamientoActual() throws Exception {
        Album album = albumPendiente();
        Long id = album.getId();
        assertThat(album.isAprobado()).isFalse();

        mockMvc.perform(put("/api/albums/" + id + "/aprobar").with(csrf()))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        // Impacto: el album quedo publicado sin pasar por el supervisor.
        Album despues = albumRepository.findById(id).orElseThrow();
        assertThat(despues.isAprobado()).isTrue();
    }

    // ------------------------------------------------------------------
    // SEC-01.b - Un rol USER elimina un album ajeno
    // ------------------------------------------------------------------
    @Test
    @WithMockUser(username = "usuario_comun", roles = "USER")
    @DisplayName("SEC-01.b - PUT /api/albums/{id}/rechazar con rol USER: comportamiento actual")
    void rechazarAlbum_conRolUser_comportamientoActual() throws Exception {
        Album album = albumPendiente();
        Long id = album.getId();

        mockMvc.perform(put("/api/albums/" + id + "/rechazar").with(csrf()))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        assertThat(albumRepository.findById(id)).isEmpty();
    }

    // ------------------------------------------------------------------
    // SEC-01.c - Un rol USER lista los albumes pendientes de revision
    // ------------------------------------------------------------------
    @Test
    @WithMockUser(username = "usuario_comun", roles = "USER")
    @DisplayName("SEC-01.c - GET /api/albums/pendientes con rol USER: comportamiento actual")
    void verPendientes_conRolUser_comportamientoActual() throws Exception {
        albumPendiente();

        mockMvc.perform(get("/api/albums/pendientes"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // Contraste: /api/admin/** SI esta protegido por el filter chain
    // ------------------------------------------------------------------
    @Test
    @WithMockUser(username = "usuario_comun", roles = "USER")
    @DisplayName("SEC-01.d - Contraste: /api/admin/pendientes con rol USER sigue devolviendo 403")
    void panelAdmin_conRolUser_siEstaProtegido() throws Exception {
        mockMvc.perform(get("/api/admin/pendientes"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Conducta esperada (habilitar tras corregir SEC-01)
    // ------------------------------------------------------------------
    /**
     * Conducta correcta segun las anotaciones @PreAuthorize ya presentes en
     * AlbumController. Correccion propuesta: agregar @EnableMethodSecurity a
     * SecurityConfig, o cubrir PUT/DELETE de /api/albums/** en el filter chain.
     */
    @Test
    @Disabled("HALLAZGO SEC-01: @PreAuthorize no se evalua. Habilitar tras la correccion.")
    @WithMockUser(username = "usuario_comun", roles = "USER")
    @DisplayName("SEC-01 - Conducta esperada: un rol USER no deberia aprobar ni rechazar albumes")
    void operacionesDeSupervisor_conRolUser_deberianSer403() throws Exception {
        Album album = albumPendiente();
        Long id = album.getId();

        mockMvc.perform(put("/api/albums/" + id + "/aprobar").with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/albums/" + id + "/rechazar").with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/albums/pendientes"))
                .andExpect(status().isForbidden());
    }
}
