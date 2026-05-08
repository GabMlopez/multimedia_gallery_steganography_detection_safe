import React from 'react';
import { Routes, Route, NavLink, useNavigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Home from './pages/Home';
import Album from './pages/Album';
import Register from './pages/Register';
import Login from './pages/Login';
import SupervisorPanel from './pages/SupervisorPanel';
import UserPanel from './pages/UserPanel';
import { ProtectedRoute } from './pages/ProtectedRoute';

// Inyectar estilos de animaciones
const style = document.createElement('style');
style.textContent = `
  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
  @keyframes pulse {
    0%, 100% {
      opacity: 1;
    }
    50% {
      opacity: 0.5;
    }
  }
`;
document.head.appendChild(style);

export default function App() {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user'));

  const handleLogout = () => {
    localStorage.removeItem('user');
    navigate('/login', { replace: true });
  };

  return (
    <div className="app-shell">
      <ToastContainer
        position="top-right"
        autoClose={3000}
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
      />
      <header className="app-header">
        <div className="brand-lockup">
          <div className="brand-mark" aria-hidden="true">MG</div>
          <div>
            <h1>Multimedia Gallery</h1>
            <p>Galería segura, privada y con control de acceso</p>
          </div>
        </div>
        <nav className="nav-list" aria-label="Navegación principal">
          <NavLink to="/" className={({ isActive }) => `nav-link${isActive ? ' is-active' : ''}`} end>
            Inicio
          </NavLink>
          {!user && (
            <>
              <NavLink to="/login" className={({ isActive }) => `nav-link${isActive ? ' is-active' : ''}`}>
                Acceder
              </NavLink>
              <NavLink to="/register" className={({ isActive }) => `nav-link${isActive ? ' is-active' : ''}`}>
                Crear cuenta
              </NavLink>
            </>
          )}
          {user?.role === 'ROLE_USER' && (
            <NavLink to="/user-panel" className={({ isActive }) => `nav-link${isActive ? ' is-active' : ''}`}>
              Mi panel
            </NavLink>
          )}
          {user?.role === 'ROLE_SUPERVISOR' && (
            <NavLink to="/supervisor" className={({ isActive }) => `nav-link${isActive ? ' is-active' : ''}`}>
              Supervisor
            </NavLink>
          )}
          {user && <span className="session-chip">{user.username}</span>}
          {user && <span className="role-chip">{user.role?.replace('ROLE_', '')}</span>}
          {user && (
            <button onClick={handleLogout} className="logout-button">
              Salir
            </button>
          )}
        </nav>
      </header>

      <main className="main app-main">
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