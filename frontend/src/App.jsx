import React from 'react';
import { Routes, Route, Link, useNavigate, Navigate } from 'react-router-dom';
import Home from './pages/Home';
import Album from './pages/Album';
import Register from './pages/Register';
import Login from './pages/Login';
import SupervisorPanel from './pages/SupervisorPanel';
import UserPanel from './pages/UserPanel';
import { ProtectedRoute } from './pages/ProtectedRoute'; // Importante

export default function App() {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user')); 

  const handleLogout = () => {
    localStorage.removeItem('user');
    // Forzamos un reload o navegación para limpiar el estado de la app
    window.location.href = '/login';
  };

  return (
    <div className="app">
      <header className="header">
        <div className="brand-block">
          <h1>Multimedia Gallery</h1>
          <span>Galería segura y privada</span>
        </div>
        <nav>
          <Link to="/">Inicio</Link>
          
          {!user && (
            <>
              <Link to="/login">Acceder</Link>
              <Link to="/register">Crear cuenta</Link>
            </>
          )}

          {/* Validación visual de roles */}
          {user?.role === 'ROLE_USER' && <Link to="/user-panel">Mi Panel</Link>}
          {user?.role === 'ROLE_SUPERVISOR' && <Link to="/supervisor">Panel Supervisor</Link>}
          
          {user && (
            <button onClick={handleLogout} style={{marginLeft: '15px', background: '#e01b24', color: 'white', border: 'none', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer'}}>
              Salir ({user.username})
            </button>
          )}
        </nav>
      </header>

      <main className="main">
        <Routes>
          {/* RUTAS ABIERTAS: Cualquiera puede entrar aquí sin loguearse */}
          <Route path="/" element={<Home />} />
          <Route path="/album" element={<Album />} />
          <Route path="/album/:id" element={<Album />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* RUTAS PRIVADAS: Solo si hay sesión y el rol coincide */}
          <Route element={<ProtectedRoute allowedRoles={['ROLE_USER']} />}>
            <Route path="/user-panel" element={<UserPanel />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={['ROLE_SUPERVISOR']} />}>
            <Route path="/supervisor" element={<SupervisorPanel />} />
          </Route>
        </Routes>
      </main>
    </div>
  );
}