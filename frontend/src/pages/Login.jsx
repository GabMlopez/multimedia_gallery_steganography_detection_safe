import React, { useState } from 'react';
import { api } from '../lib/api';
import { useNavigate } from 'react-router-dom';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);

    // SANEAMIENTO: Limpiamos espacios antes de enviar al servidor
    const cleanUsername = username.trim();
    const cleanPassword = password.trim();

    // VALIDACIÓN: Evitamos enviar campos vacíos tras el trim
    if (!cleanUsername || !cleanPassword) {
      setError('Por favor, rellena todos los campos.');
      setIsLoading(false);
      return;
    }

    try {
      await api.get('/api/auth/csrf');
      
      // Enviamos los datos ya saneados
      const res = await api.post('/api/auth/login', { 
        username: cleanUsername, 
        password: cleanPassword 
      });
      
      localStorage.setItem('user', JSON.stringify(res.data));
      window.location.href = '/'; 
    } catch (err) {
      if (err.response && err.response.status === 429) {
        setError(err.response.data);
      } else {
        setError((err.response?.data || 'Credenciales inválidas o error de conexión'));
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-view">
      {/* ... (Sección login-splash se mantiene igual) ... */}
      <section className="login-splash" aria-hidden="true">
        <h2>Bienvenido a tu galería privada</h2>
        <p>
          Ingresa para gestionar imágenes, revisar contenido protegido y mantener tus archivos bajo control.
        </p>

        <div className="login-splash__stats">
          <div><strong>+50MB</strong><span>carga segura</span></div>
          <div><strong>JWT</strong><span>sesión protegida</span></div>
          <div><strong>RBAC</strong><span>roles por usuario</span></div>
        </div>
      </section>

      <section className="login-card" aria-label="Formulario de inicio de sesión">
        <div className="login-card__header">
          <span className="login-card__eyebrow">Multimedia Gallery</span>
          <h1>Iniciar sesión</h1>
          <p>Ingresa tus credenciales para continuar.</p>
        </div>

        <form className="login-form" onSubmit={handleLogin}>
          <label className="field">
            <span>Usuario</span>
            <div className="field__control">
              <span className="field__icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" role="img" focusable="false">
                  <path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Zm0 2c-4.42 0-8 2.24-8 5v1h16v-1c0-2.76-3.58-5-8-5Z" />
                </svg>
              </span>
              <input
                value={username}
                placeholder="Ej: jmrciallo"
                onChange={e => setUsername(e.target.value)}
                autoComplete="username"
                required
              />
            </div>
          </label>

          <label className="field">
            <span>Contraseña</span>
            <div className="field__control">
              <span className="field__icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" role="img" focusable="false">
                  <path d="M17 8h-1V6a4 4 0 0 0-8 0v2H7a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2Zm-6 8.73V18a1 1 0 0 0 2 0v-1.27a2 2 0 1 0-2 0ZM10 8V6a2 2 0 0 1 4 0v2Z" />
                </svg>
              </span>
              <input
                value={password}
                type="password"
                placeholder="Escribe tu contraseña"
                autoComplete="current-password"
                onChange={e => setPassword(e.target.value)}
                required
              />
            </div>
          </label>

          <button type="submit" className="login-button" disabled={isLoading}>
            {isLoading ? 'Verificando...' : 'Entrar'}
          </button>
        </form>

        <div className="login-card__footer">
          <span>¿No tienes cuenta?</span>
          <a href="/register">Crear cuenta</a>
        </div>

        {error && <div className="login-alert">{error}</div>}
      </section>
    </div>
  );
}