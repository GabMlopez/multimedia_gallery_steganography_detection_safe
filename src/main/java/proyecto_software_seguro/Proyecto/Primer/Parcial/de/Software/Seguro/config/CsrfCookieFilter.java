package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

final class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        
        if (null != csrfToken) {

            csrfToken.getToken(); 

            if (null != csrfToken.getHeaderName()) {
                response.setHeader(csrfToken.getHeaderName(), csrfToken.getToken());
            }
        }
        filterChain.doFilter(request, response);
    }
}