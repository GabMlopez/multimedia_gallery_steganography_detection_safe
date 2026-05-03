package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.service.FileStorageService;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private FileStorageService fileService;

//    @PostMapping("/upload/{albumId}")
//    @PreAuthorize("hasRole('USER')")
//    public ResponseEntity<?> uploadImage(@PathVariable Long albumId, @RequestParam("archivos") MultipartFile[] archivos) {
//        try {
//            if (archivos == null || archivos.length == 0) {
//                return ResponseEntity.badRequest().body("No se enviaron archivos.");
//            }
//
//            int procesados = 0;
//            int enCuarentena = 0;
//
//            for (MultipartFile archivo : archivos) {
//                // 1. Validación de Integridad (Magic Numbers)
//                if (!fileService.isValidImage(archivo)) {
//                    continue; // Si un archivo es corrupto, lo salta y sigue con el resto
//                }
//
//                // 2. Análisis Profundo (Esteganografía)
//                boolean isSuspicious = fileService.detectSteganography(archivo);
//
//                if (isSuspicious) {
//                    fileService.saveToQuarantine(archivo, albumId);
//                    enCuarentena++;
//                } else {
//                    fileService.saveCleanImage(archivo, albumId);
//                }
//                procesados++;
//            }
//
//            String mensaje = String.format("Se procesaron %d archivos correctamente. (%d enviados a cuarentena por sospecha).", procesados, enCuarentena);
//            return ResponseEntity.ok(mensaje);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Error interno al subir el lote de archivos.");
//        }
//    }

    @PostMapping("/upload/{albumId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> subirArchivosAAlbumExistente(@PathVariable Long albumId,
            @RequestParam("archivos") MultipartFile[] archivos) { // Ahora recibe un arreglo

        try {
            if (archivos == null || archivos.length == 0) {
                return ResponseEntity.badRequest().body("No se enviaron archivos.");
            }

            int procesados = 0;
            int enCuarentena = 0;

            for (MultipartFile archivo : archivos) {
                // 1. Validación de Integridad (Magic Numbers)
                if (!fileService.isValidImage(archivo)) {
                    continue; // Si un archivo es corrupto, lo salta y sigue con el resto
                }

                // 2. Análisis Profundo (Esteganografía)
                boolean isSuspicious = fileService.detectSteganography(archivo);

                if (isSuspicious) {
                    fileService.saveToQuarantine(archivo, albumId);
                    enCuarentena++;
                } else {
                    fileService.saveCleanImage(archivo, albumId);
                }
                procesados++;
            }

            String mensaje = String.format("Se procesaron %d archivos correctamente. (%d enviados a cuarentena por sospecha).", procesados, enCuarentena);
            return ResponseEntity.ok(mensaje);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error interno al subir el lote de archivos.");
        }
    }
}