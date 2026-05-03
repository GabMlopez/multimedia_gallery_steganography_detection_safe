import React, { useState } from 'react'
import { api } from '../lib/api'

export default function Register() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState(null)

  const submit = async (e) => {
    e.preventDefault();
    try {
      // Forzamos la obtención del token antes del POST
      await api.get('/api/auth/csrf');
      
      const res = await api.post('/api/auth/register', { username, password, role: 'ROLE_USER' });
      setMessage("Registro exitoso: " + res.data);
    } catch (err) {
      // Capturamos el mensaje de error de tu Regex (mayúsculas, etc.)
      setMessage(err.response?.data?.message || err.response?.data || 'Error en validación');
    }
  }

  return (
    <div className="register">
      <h2>Registro</h2>
      <form onSubmit={submit}>
        <label>Usuario</label>
        <input value={username} onChange={e => setUsername(e.target.value)} />
        <label>Contraseña</label>
        <input 
          type="password" 
          placeholder="Contraseña" 
          autoComplete="new-password" 
          onChange={e => setPassword(e.target.value)} 
        />
        <button type="submit">Registrar</button>
      </form>
      {message && <p className="message">{message}</p>}
    </div>
  )
}
