import { Navigate, Outlet } from 'react-router-dom';

export const ProtectedRoute = ({ allowedRoles }) => {
  const user = JSON.parse(localStorage.getItem('user')); // Obtenemos el usuario del login

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    // Si el rol no es el adecuado, lo mandamos a la home o una página de 403
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
};