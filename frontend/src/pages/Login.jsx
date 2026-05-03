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

    try {
      await api.get('/api/auth/csrf');
      const res = await api.post('/api/auth/login', { username, password });
      
      localStorage.setItem('user', JSON.stringify(res.data));
      window.location.href = '/'; 
    } catch (err) {
      if (err.response && err.response.status === 429) {
        setError("🚨 " + err.response.data);
      } else {
        setError('❌ ' + (err.response?.data || 'Credenciales inválidas o error de conexión'));
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Estilos Modernos
  const styles = {
    wrapper: {
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '80vh',
    },
    card: {
      width: '100%',
      maxWidth: '400px',
      padding: '40px',
      background: '#fff',
      borderRadius: '12px',
      boxShadow: '0 10px 25px rgba(0,0,0,0.1)',
      textAlign: 'center',
    },
    title: {
      marginBottom: '10px',
      color: '#1a5fb4',
      fontSize: '24px',
      fontWeight: 'bold',
    },
    subtitle: {
      color: '#666',
      marginBottom: '30px',
      fontSize: '14px',
    },
    inputGroup: {
      marginBottom: '20px',
      textAlign: 'left',
    },
    input: {
      width: '100%',
      padding: '12px 15px',
      borderRadius: '8px',
      border: '1px solid #ddd',
      fontSize: '16px',
      boxSizing: 'border-box',
      outline: 'none',
      transition: 'border-color 0.3s',
    },
    button: {
      width: '100%',
      padding: '12px',
      borderRadius: '8px',
      border: 'none',
      background: isLoading ? '#95a5a6' : '#1a5fb4',
      color: 'white',
      fontSize: '16px',
      fontWeight: 'bold',
      cursor: isLoading ? 'not-allowed' : 'pointer',
      transition: 'background 0.3s',
    },
    errorBox: {
      marginTop: '20px',
      padding: '10px',
      borderRadius: '6px',
      background: '#f8d7da',
      color: '#721c24',
      fontSize: '13px',
      border: '1px solid #f5c6cb',
    }
  };

  return (
    <div style={styles.wrapper}>
      <div style={styles.card}>
        <div style={styles.title}>Login</div>
        <p style={styles.subtitle}>Ingrese sus credenciales para continuar</p>
        
        <form onSubmit={handleLogin}>
          <div style={styles.inputGroup}>
            <label style={{display: 'block', marginBottom: '5px', fontWeight: '500'}}>Usuario</label>
            <input 
              style={styles.input}
              placeholder="Ej: jmarciallo" 
              onChange={e => setUsername(e.target.value)} 
              required
            />
          </div>

          <div style={styles.inputGroup}>
            <label style={{display: 'block', marginBottom: '5px', fontWeight: '500'}}>Contraseña</label>
            <input 
              style={styles.input}
              type="password" 
              placeholder="••••••••" 
              autoComplete="current-password" 
              onChange={e => setPassword(e.target.value)} 
              required
            />
          </div>

          <button 
            type="submit" 
            style={styles.button}
            disabled={isLoading}
          >
            {isLoading ? 'Verificando...' : 'Iniciar Sesión'}
          </button>
        </form>

        {error && (
          <div style={styles.errorBox}>
            {error}
          </div>
        )}
      </div>
    </div>
  );
}