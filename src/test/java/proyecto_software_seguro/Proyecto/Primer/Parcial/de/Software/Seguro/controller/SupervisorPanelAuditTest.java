package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Album;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.AuditLog;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Image;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.ImageStatus;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AlbumRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AuditLogRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.ImageRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.service.FileStorageService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CP30 - CP35 | Panel de Supervisor y Auditoria (SQAP seccion 3.2, pags. 29-32).
 *
 * El MockMvc se construye con springSecurity() para que se aplique la cadena de
 * filtros real de SecurityConfig. Sin eso el test no atraviesa ninguna barrera de
 * autorizacion y un 403 esperado se convierte en 200 (ver hallazgo SEC-01:
 * el proyecto no declara @EnableMethodSecurity, por lo que las anotaciones
 * @PreAuthorize de los controladores no se evaluan).
 *
 * AuditService se deja como bean real y se mockea unicamente AuditLogRepository:
 * asi se puede capturar el AuditLog construido y verificar su contenido, no solo
 * que "se llamo a algo".
 */
@SpringBootTest
@ActiveProfiles("test")
class SupervisorPanelAuditTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private ImageRepository imageRepository;

    @MockitoBean
    private AlbumRepository albumRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private Album album(Long id, String titulo, boolean aprobado) {
        Album a = new Album();
        a.setId(id);
        a.setTitulo(titulo);
        a.setDescripcion("Descripcion de prueba");
        a.setAprobado(aprobado);
        return a;
    }

    private Image image(Long id, String nombre, ImageStatus estado) {
        Image i = new Image();
        i.setId(id);
        i.setNombreArchivo(nombre);
        i.setEstado(estado);
        i.setMotivoAlerta("Anomalia LSB detectada");
        return i;
    }

    /** Captura el AuditLog que AuditService envio al repositorio. */
    private AuditLog capturarLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());
        return captor.getValue();
    }

    // ------------------------------------------------------------------
    // CP30 - Acceso del supervisor a los tres endpoints del panel
    // ------------------------------------------------------------------
    @Test
    @WithMockUser(username = "supervisor", roles = "SUPERVISOR")
    @DisplayName("CP30 - Supervisor accede a pendientes, quarantine y audit-log (200 en los tres)")
    void panelSupervisor_conRolSupervisor_devuelve200EnLosTresEndpoints() throws Exception {
        Album pendiente = album(1L, "Album pendiente", false);
        Image enCuarentena = image(10L, "SUSPECT_test.jpg", ImageStatus.QUARANTINE);

        AuditLog log = new AuditLog();
        log.setUsuarioId("supervisor");
        log.setAccion("APROBAR_IMAGEN");
        log.setRecursoId("10");
        log.setRecursoTipo("IMAGE");

        when(albumRepository.findByAprobadoFalse()).thenReturn(List.of(pendiente));
        when(imageRepository.findByAlbumId(1L)).thenReturn(List.of(enCuarentena));
        when(imageRepository.findByEstado(ImageStatus.QUARANTINE)).thenReturn(List.of(enCuarentena));
        when(auditLogRepository.findAll()).thenReturn(List.of(log));

        mockMvc.perform(get("/api/admin/pendientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Album pendiente"))
                .andExpect(jsonPath("$[0].imagenes").isArray());

        mockMvc.perform(get("/api/admin/quarantine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreArchivo").value("SUSPECT_test.jpg"))
                .andExpect(jsonPath("$[0].estado").value("QUARANTINE"));

        mockMvc.perform(get("/api/admin/audit-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").value("APROBAR_IMAGEN"))
                .andExpect(jsonPath("$[0].usuarioId").value("supervisor"));
    }

    // ------------------------------------------------------------------
    // CP31 - Usuario sin privilegios bloqueado en el log de auditoria
    // ------------------------------------------------------------------
    @Test
    @WithMockUser(username = "usuario", roles = "USER")
    @DisplayName("CP31 - Usuario con rol USER recibe 403 en audit-log y no se expone ningun log")
    void auditLog_conRolUser_devuelve403SinExponerDatos() throws Exception {
        mockMvc.perform(get("/api/admin/audit-log"))
                .andExpect(status().isForbidden());

        // La barrera debe cortar antes de tocar la base de datos.
        verify(auditLogRepository, never()).findAll();
    }

    // ------------------------------------------------------------------
    // CP32 - Auditoria al aprobar un album pendiente
    // ------------------------------------------------------------------
    @Test
    @WithMockUser(username = "supervisor", roles = "SUPERVISOR")
    @DisplayName("CP32 - Aprobar album marca aprobado=true y registra AuditLog APROBAR_ALBUM")
    void aprobarAlbum_registraAuditLog() throws Exception {
        Album pendiente = album(1L, "Album pendiente", false);
        when(albumRepository.findById(1L)).thenReturn(Optional.of(pendiente));

        mockMvc.perform(post("/api/admin/albums/1/aprobar").with(csrf()))
                .andExpect(status().isOk());

        ArgumentCaptor<Album> albumCaptor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue().isAprobado()).isTrue();

        AuditLog log = capturarLog();
        assertThat(log.getAccion()).isEqualTo("APROBAR_ALBUM");
        assertThat(log.getUsuarioId()).isEqualTo("supervisor");
        assertThat(log.getRecursoId()).isEqualTo("1");
        assertThat(log.getRecursoTipo()).isEqualTo("ALBUM");
        assertThat(log.getDetalles()).contains("Album pendiente");
        assertThat(log.getIpAddress()).isNotBlank();
    }

    // ------------------------------------------------------------------
    // CP33 - Auditoria al rechazar un album con imagenes asociadas
    // ------------------------------------------------------------------
    @Test
    @WithMockUser(username = "supervisor", roles = "SUPERVISOR")
    @DisplayName("CP33 - Rechazar album borra las imagenes fisicas y registra RECHAZAR_ALBUM")
    void rechazarAlbum_borraArchivosYRegistraAuditLog() throws Exception {
        Album pendiente = album(1L, "Album a rechazar", false);

        // Archivos reales en un directorio temporal para comprobar el borrado fisico.
        Path archivo1 = Files.writeString(tempDir.resolve("img1.jpg"), "contenido-1");
        Path archivo2 = Files.writeString(tempDir.resolve("img2.jpg"), "contenido-2");

        Image img1 = image(10L, "img1.jpg", ImageStatus.QUARANTINE);
        img1.setRutaLocal(archivo1.toString());
        Image img2 = image(11L, "img2.jpg", ImageStatus.QUARANTINE);
        img2.setRutaLocal(archivo2.toString());

        when(albumRepository.findById(1L)).thenReturn(Optional.of(pendiente));
        when(imageRepository.findByAlbumId(1L)).thenReturn(List.of(img1, img2));

        mockMvc.perform(delete("/api/admin/albums/1/rechazar").with(csrf()))
                .andExpect(status().isOk());

        assertThat(Files.exists(archivo1)).isFalse();
        assertThat(Files.exists(archivo2)).isFalse();

        verify(imageRepository).deleteAll(anyList());
        verify(albumRepository).delete(pendiente);

        AuditLog log = capturarLog();
        assertThat(log.getAccion()).isEqualTo("RECHAZAR_ALBUM");
        assertThat(log.getUsuarioId()).isEqualTo("supervisor");
        assertThat(log.getRecursoId()).isEqualTo("1");
        assertThat(log.getRecursoTipo()).isEqualTo("ALBUM");
        assertThat(log.getDetalles()).contains("Album a rechazar");
    }

    // ------------------------------------------------------------------
    // CP34 - Auditoria al aprobar una imagen en cuarentena
    // ------------------------------------------------------------------
    @Test
    @WithMockUser(username = "supervisor", roles = "SUPERVISOR")
    @DisplayName("CP34 - Aprobar imagen en cuarentena registra AuditLog APROBAR_IMAGEN")
    void aprobarImagen_registraAuditLog() throws Exception {
        Image enCuarentena = image(5L, "SUSPECT_foto.jpg", ImageStatus.QUARANTINE);
        when(imageRepository.findById(5L)).thenReturn(Optional.of(enCuarentena));
        doNothing().when(fileStorageService).approveQuarantinedImage(5L);

        mockMvc.perform(put("/api/admin/image/5/approve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Imagen aprobada y enviada a la bóveda segura."));

        verify(fileStorageService, times(1)).approveQuarantinedImage(5L);

        AuditLog log = capturarLog();
        assertThat(log.getAccion()).isEqualTo("APROBAR_IMAGEN");
        assertThat(log.getUsuarioId()).isEqualTo("supervisor");
        assertThat(log.getRecursoId()).isEqualTo("5");
        assertThat(log.getRecursoTipo()).isEqualTo("IMAGE");
        assertThat(log.getDetalles()).contains("SUSPECT_foto.jpg");

        // El panel debe reflejar que la amenaza fue neutralizada.
        when(imageRepository.findByEstado(ImageStatus.QUARANTINE)).thenReturn(List.of());
        mockMvc.perform(get("/api/admin/quarantine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------------
    // CP35 - Auditoria al rechazar/bloquear una imagen en cuarentena
    // ------------------------------------------------------------------
    @Test
    @WithMockUser(username = "supervisor", roles = "SUPERVISOR")
    @DisplayName("CP35 - Rechazar imagen en cuarentena registra AuditLog RECHAZAR_IMAGEN")
    void rechazarImagen_registraAuditLog() throws Exception {
        Image enCuarentena = image(5L, "SUSPECT_malware.jpg", ImageStatus.QUARANTINE);
        when(imageRepository.findById(5L)).thenReturn(Optional.of(enCuarentena));
        doNothing().when(fileStorageService).rejectQuarantinedImage(5L);

        mockMvc.perform(put("/api/admin/image/5/reject").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Imagen rechazada y eliminada de los servidores."));

        verify(fileStorageService, times(1)).rejectQuarantinedImage(5L);

        AuditLog log = capturarLog();
        assertThat(log.getAccion()).isEqualTo("RECHAZAR_IMAGEN");
        assertThat(log.getUsuarioId()).isEqualTo("supervisor");
        assertThat(log.getRecursoId()).isEqualTo("5");
        assertThat(log.getRecursoTipo()).isEqualTo("IMAGE");
        assertThat(log.getDetalles()).contains("SUSPECT_malware.jpg");
    }
}
