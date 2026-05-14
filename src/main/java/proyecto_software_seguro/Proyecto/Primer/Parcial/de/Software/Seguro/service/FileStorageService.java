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
import java.security.MessageDigest;
import java.util.*;

@Service
public class FileStorageService {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private AlbumRepository albumRepository;

    private final String UPLOAD_DIR = "uploads/safe/";
    private final String QUARANTINE_DIR = "uploads/quarantine/";

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

    private String calcularHashSHA256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] byteArray = new byte[1024];
            int bytesCount;

            while ((bytesCount = inputStream.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }

            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular el hash del archivo", e);
        }
    }

    public boolean isValidImage(MultipartFile file) {
        try {
            if (file.isEmpty()) return false;

            byte[] magicNumbers = new byte[4];
            try (InputStream is = file.getInputStream()) {
                if (is.read(magicNumbers) != 4) return false;
            }

            StringBuilder hexString = new StringBuilder();
            for (byte b : magicNumbers) {
                hexString.append(String.format("%02X", b));
            }
            String hex = hexString.toString().toUpperCase();

            System.out.println("Hexadecimal del archivo recibido: " + hex);

            boolean isJPEG = hex.startsWith("FFD8FF");
            boolean isPNG = hex.startsWith("89504E47");
            boolean isPDF = hex.startsWith("25504446");

            return isJPEG || isPNG || isPDF;

        } catch (IOException e) {
            System.out.println("Error al leer el archivo: " + e.getMessage());
            return false;
        }
    }

    public byte[] cleanImage(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(originalImage, "jpg", outputStream);
        return outputStream.toByteArray();
    }

    @Transactional
    public void saveCleanImage(MultipartFile file, Long albumId, String hash) throws IOException {
        byte[] cleanBytes = cleanImage(file);
        String filename = UUID.randomUUID().toString() + ".jpg";

        Path path = Paths.get(UPLOAD_DIR + filename);
        Files.write(path, cleanBytes);

        saveImageData(filename, path.toString(), ImageStatus.CLEAN, albumId, null, hash);
    }

    @Transactional
    public void saveToQuarantine(MultipartFile file, Long albumId, String alert, String hash) throws IOException {
        String filename = "SUSPECT_" + UUID.randomUUID().toString() + ".jpg";
        Path path = Paths.get(QUARANTINE_DIR + filename);

        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        saveImageData(filename, path.toString(), ImageStatus.QUARANTINE, albumId, alert, hash);
    }

    // Pasamos el parámetro hash para guardarlo en la Base de Datos
    private void saveImageData(String name, String path, ImageStatus status, Long albumId, String alert, String hash) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Álbum no encontrado"));

        Image img = new Image();
        img.setNombreArchivo(name);
        img.setRutaLocal(path);
        img.setEstado(status);
        img.setMotivoAlerta(alert);
        img.setAlbum(album);

        imageRepository.save(img);
    }

    public boolean detectSteganography(MultipartFile file) {
        try {
            if (hasEOFAnomaly(file)) return true;
            if (hasMetadataInconsistency(file)) return true;
            if (hasLSBAnomaly(file)) {
                System.out.println("Alerta: Anomalía LSB detectada en la imagen.");
                return true;
            }
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private boolean hasLSBAnomaly(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) return false;

            int width = image.getWidth();
            int bitChanges = 0;
            int sampleSize = Math.min(width, 100);

            for (int x = 0; x < sampleSize - 1; x++) {
                int pixel1 = image.getRGB(x, 0);
                int pixel2 = image.getRGB(x + 1, 0);

                int lsb1 = (pixel1 & 1);
                int lsb2 = (pixel2 & 1);

                if (lsb1 != lsb2) bitChanges++;
            }

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

        Files.deleteIfExists(Paths.get(image.getRutaLocal()));
        imageRepository.delete(image);
    }

    @Transactional
    public void guardarImagenesEnAlbum(Long albumId, MultipartFile[] archivos) {
        albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Álbum no encontrado"));

        Set<String> hashesProcesadosEnLote = new HashSet<>();

        for (MultipartFile archivo : archivos) {
            if (archivo.isEmpty()) continue;

            try {
                if (!isValidImage(archivo)) {
                    System.out.println(" Archivo inválido (Magic Numbers rechazados): " + archivo.getOriginalFilename());
                    continue;
                }

                String hashArchivo = calcularHashSHA256(archivo.getInputStream());

                if (hashesProcesadosEnLote.contains(hashArchivo)) {
                    System.out.println("Duplicado ignorado en el lote actual: " + archivo.getOriginalFilename());
                    continue;
                }

                boolean yaExisteEnBD = imageRepository.existsByAlbumIdAndHash(albumId, hashArchivo);
                if (yaExisteEnBD) {
                    System.out.println(" El archivo ya existe en este álbum (DB): " + archivo.getOriginalFilename());
                    continue;
                }

                if (detectSteganography(archivo)) {
                    String reporteAlertas = getSteganographyReport(archivo);
                    String alertaCompleta = (reporteAlertas != null ? reporteAlertas : "Anomalía estructural") + " | Hash: " + hashArchivo;

                    saveToQuarantine(archivo, albumId, alertaCompleta, hashArchivo);
                    System.out.println("Archivo enviado a CUARENTENA: " + archivo.getOriginalFilename());
                } else {
                    saveCleanImage(archivo, albumId, hashArchivo);
                    System.out.println("Archivo procesado y almacenado como SEGURO: " + archivo.getOriginalFilename());
                }

                hashesProcesadosEnLote.add(hashArchivo);

            } catch (Exception e) {
                System.err.println("Error crítico procesando el archivo " + archivo.getOriginalFilename() + ": " + e.getMessage());
            }
        }
    }

    public boolean hasEOFAnomaly(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        if (bytes.length < 2) return false;

        int eofIndex = -1;
        for (int i = 0; i < bytes.length - 1; i++) {
            if ((bytes[i] & 0xFF) == 0xFF && (bytes[i + 1] & 0xFF) == 0xD9) {
                eofIndex = i + 2;
                break;
            }
        }
        return (eofIndex != -1 && (bytes.length - eofIndex) > 10);
    }

    private boolean hasMetadataInconsistency(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.ISO_8859_1);
        return content.contains("PK\u0003\u0004");
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