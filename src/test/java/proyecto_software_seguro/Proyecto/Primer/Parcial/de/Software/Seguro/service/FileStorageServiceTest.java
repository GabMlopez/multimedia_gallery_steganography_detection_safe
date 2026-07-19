package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AlbumRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.ImageRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @InjectMocks
    private FileStorageService fileStorageService;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private AlbumRepository albumRepository;

    private byte[] createMinimalJPEG() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(new byte[]{
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                    0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                    0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
                    (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08,
                    0x06, 0x06, 0x07, 0x06, 0x05, 0x08, 0x07, 0x07,
                    0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C, 0x14, 0x0D,
                    0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F,
                    0x14, 0x1D, 0x1A, 0x1F, 0x1E, 0x1D, 0x1A, 0x1C,
                    0x1C, 0x20, 0x24, 0x2E, 0x27, 0x20, 0x22, 0x2C,
                    0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29, 0x2C, 0x30,
                    0x31, 0x34, 0x34, 0x34, 0x1F, 0x27, 0x39, 0x3D,
                    0x38, 0x32, 0x3C, 0x2E, 0x33, 0x34, 0x32,
                    (byte) 0xFF, (byte) 0xC0, 0x00, 0x0B, 0x08, 0x00,
                    0x01, 0x00, 0x01, 0x01, 0x01, 0x11, 0x00,
                    (byte) 0xFF, (byte) 0xC4, 0x00, 0x1F, 0x00, 0x00,
                    0x01, 0x05, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
                    0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x0A, 0x0B,
                    (byte) 0xFF, (byte) 0xD9
            });
        } catch (IOException e) {
            fail("Error creating test JPEG");
        }
        return baos.toByteArray();
    }

    @Test
    @DisplayName("isValidImage - JPEG valido debe retornar true")
    void isValidImage_withValidJPEG_returnsTrue() {
        MultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", createMinimalJPEG()
        );
        assertTrue(fileStorageService.isValidImage(file));
    }

    @Test
    @DisplayName("isValidImage - archivo vacio debe retornar false")
    void isValidImage_withEmptyFile_returnsFalse() {
        MultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );
        assertFalse(fileStorageService.isValidImage(file));
    }

    @Test
    @DisplayName("isValidImage - archivo invalido (texto) debe retornar false")
    void isValidImage_withInvalidFile_returnsFalse() {
        MultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hola mundo".getBytes()
        );
        assertFalse(fileStorageService.isValidImage(file));
    }

    @Test
    @DisplayName("detectSteganography - JPEG limpio sin esteganografia retorna false")
    void detectSteganography_withCleanJPEG_returnsFalse() {
        MultipartFile file = new MockMultipartFile(
                "file", "clean.jpg", "image/jpeg", createMinimalJPEG()
        );
        assertFalse(fileStorageService.detectSteganography(file));
    }

    @Test
    @DisplayName("detectSteganography - JPEG con datos post-EOF retorna true (contenido no danino)")
    void detectSteganography_withEOFAnomaly_returnsTrue() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(createMinimalJPEG());
            baos.write("ESTE_ES_UN_MENSAJE_OCULTO_NO_DANINO".getBytes());
        } catch (IOException e) {
            fail("Error creating test data");
        }

        MultipartFile file = new MockMultipartFile(
                "file", "stego.jpg", "image/jpeg", baos.toByteArray()
        );
        assertTrue(fileStorageService.detectSteganography(file));
    }

    @Test
    @DisplayName("detectSteganography - JPEG con firma ZIP interna retorna true (contenido danino)")
    void detectSteganography_withPKZipSignature_returnsTrue() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(createMinimalJPEG());
            baos.write(new byte[]{0x50, 0x4B, 0x03, 0x04});
            baos.write("fake_exe_inside".getBytes());
        } catch (IOException e) {
            fail("Error creating test data");
        }

        MultipartFile file = new MockMultipartFile(
                "file", "infected.jpg", "image/jpeg", baos.toByteArray()
        );
        assertTrue(fileStorageService.detectSteganography(file));
    }

    @Test
    @DisplayName("hasEOFAnomaly - datos post-EOF menor a 10 bytes no se considera anomalia")
    void hasEOFAnomaly_withSmallDataAfterEOF_returnsFalse() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(createMinimalJPEG());
        baos.write("corto".getBytes());

        MultipartFile file = new MockMultipartFile(
                "file", "small.jpg", "image/jpeg", baos.toByteArray()
        );
        assertFalse(fileStorageService.hasEOFAnomaly(file));
    }

    @Test
    @DisplayName("hasEOFAnomaly - sin datos post-EOF retorna false")
    void hasEOFAnomaly_withNoDataAfterEOF_returnsFalse() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "file", "clean.jpg", "image/jpeg", createMinimalJPEG()
        );
        assertFalse(fileStorageService.hasEOFAnomaly(file));
    }

    @Test
    @DisplayName("getSteganographyReport - JPEG con datos post-EOF genera report")
    void getSteganographyReport_withEOFAnomaly_returnsReport() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(createMinimalJPEG());
            baos.write("MENSAJE_OCULTO_DEL_AUTOR".getBytes());
        } catch (IOException e) {
            fail("Error creating test data");
        }

        MultipartFile file = new MockMultipartFile(
                "file", "stego.jpg", "image/jpeg", baos.toByteArray()
        );
        String report = fileStorageService.getSteganographyReport(file);
        assertNotNull(report);
        assertTrue(report.contains("EOF"));
    }

    @Test
    @DisplayName("getSteganographyReport - JPEG limpio retorna null")
    void getSteganographyReport_withCleanJPEG_returnsNull() {
        MultipartFile file = new MockMultipartFile(
                "file", "clean.jpg", "image/jpeg", createMinimalJPEG()
        );
        assertNull(fileStorageService.getSteganographyReport(file));
    }
}
