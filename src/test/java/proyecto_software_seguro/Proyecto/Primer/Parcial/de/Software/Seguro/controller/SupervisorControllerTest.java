package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Image;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.ImageStatus;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AlbumRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AuditLogRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.ImageRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.service.FileStorageService;

import org.springframework.security.test.context.support.WithMockUser;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class SupervisorControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @MockitoBean
    private ImageRepository imageRepository;

    @MockitoBean
    private AlbumRepository albumRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("GET /api/admin/quarantine - sin autenticacion retorna 403")
    void getQuarantineGallery_withoutAuth_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/quarantine"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "supervisor", roles = "SUPERVISOR")
    @DisplayName("GET /api/admin/quarantine - supervisor retorna lista")
    void getQuarantineGallery_withSupervisorRole_returnsList() throws Exception {
        Image img = new Image();
        img.setId(1L);
        img.setNombreArchivo("SUSPECT_test.jpg");
        img.setEstado(ImageStatus.QUARANTINE);
        img.setMotivoAlerta("Anomalia LSB detectada");

        when(imageRepository.findByEstado(ImageStatus.QUARANTINE))
                .thenReturn(List.of(img));

        mockMvc.perform(get("/api/admin/quarantine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreArchivo").value("SUSPECT_test.jpg"))
                .andExpect(jsonPath("$[0].estado").value("QUARANTINE"))
                .andExpect(jsonPath("$[0].motivoAlerta").value("Anomalia LSB detectada"));
    }

    @Test
    @WithMockUser(username = "supervisor", roles = "SUPERVISOR")
    @DisplayName("PUT /api/admin/image/{id}/approve - aprueba imagen en cuarentena")
    void approveImage_validId_returnsOk() throws Exception {
        Image image = new Image();
        image.setId(1L);
        image.setNombreArchivo("SUSPECT_test.jpg");
        image.setEstado(ImageStatus.QUARANTINE);

        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        doNothing().when(fileStorageService).approveQuarantinedImage(1L);

        mockMvc.perform(put("/api/admin/image/1/approve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Imagen aprobada y enviada a la b\u00F3veda segura."));

        verify(fileStorageService, times(1)).approveQuarantinedImage(1L);
    }

    @Test
    @WithMockUser(username = "supervisor", roles = "SUPERVISOR")
    @DisplayName("PUT /api/admin/image/{id}/approve - imagen inexistente retorna 404")
    void approveImage_invalidId_returnsNotFound() throws Exception {
        when(imageRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin/image/999/approve").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Imagen no encontrada"));
    }

    @Test
    @WithMockUser(username = "supervisor", roles = "SUPERVISOR")
    @DisplayName("PUT /api/admin/image/{id}/reject - rechaza imagen en cuarentena")
    void rejectImage_validId_returnsOk() throws Exception {
        Image image = new Image();
        image.setId(1L);
        image.setNombreArchivo("SUSPECT_test.jpg");
        image.setEstado(ImageStatus.QUARANTINE);

        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        doNothing().when(fileStorageService).rejectQuarantinedImage(1L);

        mockMvc.perform(put("/api/admin/image/1/reject").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Imagen rechazada y eliminada de los servidores."));

        verify(fileStorageService, times(1)).rejectQuarantinedImage(1L);
    }

    @Test
    @WithMockUser(username = "supervisor", roles = "SUPERVISOR")
    @DisplayName("PUT /api/admin/image/{id}/reject - imagen inexistente retorna 404")
    void rejectImage_invalidId_returnsNotFound() throws Exception {
        when(imageRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin/image/999/reject").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Imagen no encontrada"));
    }

    @Test
    @WithMockUser(username = "supervisor", roles = "SUPERVISOR")
    @DisplayName("GET /api/admin/quarantine/view/{filename} - path traversal bloqueado")
    void serveQuarantinedImage_pathTraversal_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/quarantine/view/../../../etc/passwd"))
                .andExpect(status().is4xxClientError());
    }
}
