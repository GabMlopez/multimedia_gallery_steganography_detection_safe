package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.util;

import com.google.common.html.HtmlEscapers;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class XssSanitizer {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements("b", "i", "em", "strong", "p", "br", "a")
            .allowAttributes("href").matching(java.util.regex.Pattern.compile("^https?://.*"))
            .onElements("a")
            .toFactory();

    /**
     * Sanitiza texto para prevenir XSS
     * Permite solo elementos HTML seguros (b, i, em, strong, p, br, a)
     */
    public static String sanitize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        return POLICY.sanitize(input);
    }

    /**
     * Escapa caracteres HTML sin permitir ningún tag
     * Más restrictivo que sanitize()
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return HtmlEscapers.htmlEscaper().escape(input);
    }
}
