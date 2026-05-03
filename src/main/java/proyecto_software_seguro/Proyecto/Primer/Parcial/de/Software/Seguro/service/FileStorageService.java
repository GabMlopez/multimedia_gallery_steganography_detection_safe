package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Album;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.Image;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.ImageStatus;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AlbumRepository;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.ImageRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private AlbumRepository albumRepository;

    private final String UPLOAD_DIR = "uploads/safe/";
    private final String QUARANTINE_DIR = "uploads/quarantine/";

    // El límite físico real lo maneja application.properties,
    // pero esta constante sirve para lógica interna si la necesitas.
    private final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

    // ¡CRÍTICO! @PostConstruct hace que este método se ejecute automáticamente al iniciar Spring Boot
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get("uploads/safe"));
            Files.createDirectories(Paths.get("uploads/quarantine"));
            System.out.println("Directorios de almacenamiento seguro inicializados.");
        } catch (IOException e) {
            throw new RuntimeException("No se pudieron inicializar las carpetas de almacenamiento", e);
        }
    }

    public boolean isValidImage(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return false;
            }

            byte[] magicNumbers = new byte[4];
            InputStream is = file.getInputStream();
            if (is.read(magicNumbers) != 4) {
                return false;
            }
            is.close();

            // Convertir bytes a Hexadecimal asegurando que sea MAYÚSCULA
            StringBuilder hexString = new StringBuilder();
            for (byte b : magicNumbers) {
                hexString.append(String.format("%02X", b));
            }
            String hex = hexString.toString().toUpperCase();

            System.out.println("Hexadecimal del archivo recibido: " + hex);

            // RF: Validación de Integridad de Archivos (Magic Numbers)
            boolean isJPEG = hex.startsWith("FFD8FF");
            boolean isPNG = hex.startsWith("89504E47");
            boolean isPDF = hex.startsWith("25504446"); // Soporte para documentos PDF

            return isJPEG || isPNG || isPDF;

        } catch (IOException e) {
            System.out.println("Error al leer el archivo: " + e.getMessage());
            return false;
        }
    }

    public byte[] cleanImage(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Forza la reescritura de la imagen, eliminando payloads adjuntos al final del archivo
        ImageIO.write(originalImage, "jpg", outputStream);
        return outputStream.toByteArray();
    }

    @Transactional
    public void saveCleanImage(MultipartFile file, Long albumId) throws IOException {
        byte[] cleanBytes = cleanImage(file);
        // Usar UUID previene ataques de Path Traversal y sobreescritura de archivos
        String filename = UUID.randomUUID().toString() + ".jpg";

        Path path = Paths.get(UPLOAD_DIR + filename);
        Files.write(path, cleanBytes);

        saveImageData(filename, path.toString(), ImageStatus.CLEAN, albumId, null);
    }

    @Transactional
    public void saveToQuarantine(MultipartFile file, Long albumId) throws IOException {
        String filename = "SUSPECT_" + UUID.randomUUID().toString() + ".jpg";
        Path path = Paths.get(QUARANTINE_DIR + filename);

        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        saveImageData(filename, path.toString(), ImageStatus.QUARANTINE, albumId, "Anomalía estructural EOF detectada");
    }

    private void saveImageData(String name, String path, ImageStatus status, Long albumId, String alert) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Álbum no encontrado"));

        Image img = new Image();
        img.setNombreArchivo(name); // Aquí aseguramos que el nombre nunca sea null
        img.setRutaLocal(path);
        img.setEstado(status);
        img.setMotivoAlerta(alert);
        img.setAlbum(album);

        imageRepository.save(img);
    }

    public boolean detectSteganography(MultipartFile file) {
        try {
            // 1. Análisis de Marcador EOF (Detecta payload adjunto al final)
            if (hasEOFAnomaly(file)) {
                return true;
            }

            // 2. Validación de Consistencia de Metadatos (Detecta ZIPs ocultos)
            if (hasMetadataInconsistency(file)) {
                return true;
            }

            // 3. NUEVO: Análisis de Ruido LSB (Least Significant Bit)
            if (hasLSBAnomaly(file)) {
                System.out.println("⚠️ Alerta: Anomalía LSB detectada en la imagen.");
                return true;
            }

            return false;
        } catch (IOException e) {
            return true; // Fail Secure
        }
    }

    private boolean hasLSBAnomaly(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) return false;

            int width = image.getWidth();
            int height = image.getHeight();

            // Analizamos una muestra de píxeles (ej. la primera fila) para no saturar el CPU
            int sampleSize = Math.min(width, 100);
            int bitChanges = 0;

            for (int x = 0; x < sampleSize - 1; x++) {
                int pixel1 = image.getRGB(x, 0);
                int pixel2 = image.getRGB(x + 1, 0);

                // Extraemos el bit menos significativo (LSB) del canal azul de dos píxeles adyacentes
                int lsb1 = (pixel1 & 1);
                int lsb2 = (pixel2 & 1);

                // Si el LSB fluctúa de forma no natural y constante (ruido blanco), es síntoma de esteganografía
                if (lsb1 != lsb2) {
                    bitChanges++;
                }
            }

            // Si más del 85% de la muestra tiene fluctuaciones en el LSB, es altamente anómalo
            double variationRate = (double) bitChanges / sampleSize;
            return variationRate > 0.85;

        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public void approveQuarantinedImage(Long imageId) throws IOException {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada"));

        Path source = Paths.get(image.getRutaLocal());
        String newFilename = image.getNombreArchivo().replace("SUSPECT_", "");
        Path target = Paths.get(UPLOAD_DIR + newFilename);

        // Mover archivo físico a la bóveda segura
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

        image.setEstado(ImageStatus.CLEAN);
        image.setRutaLocal(target.toString());
        image.setNombreArchivo(newFilename);
        image.setMotivoAlerta("Aprobado manualmente por Supervisor");
        imageRepository.save(image);
    }

    @Transactional
    public void rejectQuarantinedImage(Long imageId) throws IOException {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada"));

        // Elimina el archivo del sistema de ficheros de Render/Local
        Files.deleteIfExists(Paths.get(image.getRutaLocal()));

        // Elimina el rastro de la base de datos
        imageRepository.delete(image);
    }

    public boolean hasEOFAnomaly(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        if (bytes.length < 2) return false;

        int eofIndex = -1;
        for (int i = 0; i < bytes.length - 1; i++) {
            // Busca la marca de fin de archivo JPEG (FF D9)
            if ((bytes[i] & 0xFF) == 0xFF && (bytes[i + 1] & 0xFF) == 0xD9) {
                eofIndex = i + 2;
                break;
            }
        }
        // Si hay más de 10 bytes de información después del fin de archivo, es sospechoso
        return (eofIndex != -1 && (bytes.length - eofIndex) > 10);
    }

    private boolean hasMetadataInconsistency(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.ISO_8859_1);
        return content.contains("PK\u0003\u0004"); // Firma típica de archivos ZIP ocultos.
    }

    public String getSteganographyReport(MultipartFile file) {
        List<String> findings = new ArrayList<>();
        try {
            if (hasEOFAnomaly(file)) {
                findings.add("Ataque de Marcador EOF: Datos detectados tras el fin de estructura (FF D9).");
            }
            if (hasMetadataInconsistency(file)) {
                findings.add("Firma de contenedor (PK/Zip) detectada dentro de la estructura de imagen.");
            }
            if (hasLSBAnomaly(file)) {
                findings.add("Anomalía LSB: Ruido estadístico inconsistente en bits menos significativos.");
            }
        } catch (IOException e) {
            return "Error de integridad: El archivo no pudo ser leído completamente.";
        }
        return findings.isEmpty() ? null : String.join(" | ", findings);
    }
}