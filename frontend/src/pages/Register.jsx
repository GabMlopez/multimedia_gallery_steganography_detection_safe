import React, { useState } from 'react'
import { api } from '../lib/api'
import { notifySuccess, notifyError } from '../lib/notifications'
import { LoadingSpinner } from '../components/LoadingSpinner'
import { PasswordValidator, isPasswordValid } from '../components/PasswordValidator'

export default function Register() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault();
    
    if (!isPasswordValid(password)) {
      notifyError('La contraseña debe tener: mayúscula, carácter especial y 8+ caracteres');
      return;
    }
    
    setIsLoading(true)
    try {

      await api.get('/api/auth/csrf');
      
      const res = await api.post('/api/auth/register', { username, password, role: 'ROLE_USER' });
      notifySuccess('Registro exitoso. Redirigiendo a login...');
      setTimeout(() => {
        window.location.href = '/login';
      }, 2000);
    } catch (err) {

      notifyError(err.response?.data?.message || err.response?.data || 'Error en validación');
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="register-view">
      <section className="register-panel">
        <div className="register-panel__header">
          <span className="login-card__eyebrow">Nuevo usuario</span>
          <h2>Crear cuenta</h2>
          <p>Completa tus datos para empezar a usar la galería de forma segura.</p>
        </div>

        <form className="register-form" onSubmit={submit}>
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
                onChange={e => setUsername(e.target.value)}
                placeholder="Elige un nombre de usuario"
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
                type="password" 
                placeholder="Crea una contraseña" 
                autoComplete="new-password" 
                value={password}
                onChange={e => setPassword(e.target.value)} 
                required
              />
            </div>
          </label>

          {password && <PasswordValidator password={password} />}

          <button className="register-button" type="submit" disabled={isLoading}>
            {isLoading ? (
              <>
                <LoadingSpinner /> Registrando...
              </>
            ) : (
              'Registrar'
            )}
          </button>
        </form>
      </section>
    </div>
  )
}
