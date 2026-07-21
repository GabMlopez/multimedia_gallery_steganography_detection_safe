package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.cli;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class DetectSteganographyCli {

    public static void main(String[] args) {
        Path projectRoot = Paths.get("");
        Path uploads = projectRoot.resolve("uploads");
        Path safe = uploads.resolve("safe");
        Path quarantine = uploads.resolve("quarantine");

        try {
            Files.createDirectories(projectRoot.resolve("reports"));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path reportFile = projectRoot.resolve("reports").resolve("steg_report_" + timestamp + ".txt");

            List<String> reportLines = new ArrayList<>();
            reportLines.add("Steganography scan report - " + timestamp);
            reportLines.add("====================================");

            if (Files.exists(safe)) {
                reportLines.add("\n-- SAFER FOLDER: uploads/safe --");
                scanDirectory(safe, reportLines);
            } else {
                reportLines.add("uploads/safe not found, skipping.");
            }

            if (Files.exists(quarantine)) {
                reportLines.add("\n-- QUARANTINE FOLDER: uploads/quarantine --");
                scanDirectory(quarantine, reportLines);
            } else {
                reportLines.add("uploads/quarantine not found, skipping.");
            }

            Files.write(reportFile, reportLines, StandardCharsets.UTF_8, StandardOpenOption.CREATE);

            System.out.println("Reporte generado en: " + reportFile.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error generando reporte: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void scanDirectory(Path dir, List<String> reportLines) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    List<String> findings = analyzeFile(p);
                    reportLines.add(p.getFileName() + " | " + p.toAbsolutePath());
                    if (findings.isEmpty()) {
                        reportLines.add("  => CLEAN");
                    } else {
                        for (String f : findings) reportLines.add("  => " + f);
                    }
                }
            }
        }
    }

    private static List<String> analyzeFile(Path file) {
        List<String> findings = new ArrayList<>();
        try {
            if (hasEOFAnomaly(file)) findings.add("Ataque de Marcador EOF: Datos detectados tras FF D9");
            if (hasMetadataInconsistency(file)) findings.add("Firma de contenedor (PK/Zip) detectada dentro de la estructura de imagen");
            if (hasLSBAnomaly(file)) findings.add("Anomalía LSB: Ruido estadístico inconsistente en bits menos significativos");
        } catch (Exception e) {
            findings.add("Error leyendo el archivo: " + e.getMessage());
        }
        return findings;
    }

    private static boolean hasEOFAnomaly(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
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

    private static boolean hasMetadataInconsistency(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String content = new String(bytes, StandardCharsets.ISO_8859_1);
        return content.contains("PK\u0003\u0004");
    }

    private static boolean hasLSBAnomaly(Path file) {
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) return false;

            int width = image.getWidth();
            int sampleSize = Math.min(width, 100);
            int bitChanges = 0;

            for (int x = 0; x < sampleSize - 1; x++) {
                int pixel1 = image.getRGB(x, 0);
                int pixel2 = image.getRGB(x + 1, 0);

                int lsb1 = (pixel1 & 1);
                int lsb2 = (pixel2 & 1);

                if (lsb1 != lsb2) bitChanges++;
            }

            double variationRate = (double) bitChanges / (double) sampleSize;
            return variationRate > 0.85;
        } catch (Exception e) {
            return false;
        }
    }
}
