package proyecto_software_seguro.Proyecto.Primer.Parcial.de.Software.Seguro.service;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final int MAX_ATTEMPTS = 5;
    private final long LOCK_TIME_DURATION = 2 * 60 * 1000; // 15 minutos en milisegundos

    // Almacena: Username -> Número de intentos fallidos
    private final ConcurrentHashMap<String, Integer> attemptsCache = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> lockCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String username) {
        attemptsCache.remove(username);
        lockCache.remove(username);
    }

    public void loginFailed(String username) {
        int attempts = attemptsCache.getOrDefault(username, 0);
        attempts++;
        attemptsCache.put(username, attempts);

        if (attempts >= MAX_ATTEMPTS) {
            lockCache.put(username, System.currentTimeMillis());
        }
    }

    public boolean isBlocked(String username) {
        if (!lockCache.containsKey(username)) {
            return false;
        }

        long lockTime = lockCache.get(username);
        if (System.currentTimeMillis() - lockTime > LOCK_TIME_DURATION) {
            lockCache.remove(username);
            attemptsCache.remove(username);
            return false;
        }

        return true;
    }
}
