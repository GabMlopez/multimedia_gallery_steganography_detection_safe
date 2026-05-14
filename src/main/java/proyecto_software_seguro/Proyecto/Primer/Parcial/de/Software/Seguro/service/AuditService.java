package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.entity.AuditLog;
import proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.repository.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void logAction(String usuarioId, String accion, String recursoId, String recursoTipo, String detalles) {
        AuditLog log = new AuditLog();
        log.setUsuarioId(usuarioId);
        log.setAccion(accion);
        log.setRecursoId(recursoId);
        log.setRecursoTipo(recursoTipo);
        log.setDetalles(detalles);
        log.setIpAddress(getClientIpAddress());

        auditLogRepository.save(log);
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "UNKNOWN";

            HttpServletRequest request = attrs.getRequest();
            String xForwarded = request.getHeader("X-Forwarded-For");
            if (xForwarded != null && !xForwarded.isEmpty()) {
                return xForwarded.split(",")[0];
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
