import React, { useState, useEffect } from 'react';
import { api, apiUrl } from '../lib/api';
import ProtectedImage from '../pages/ProtectedImage';
import ConfirmModal from '../components/ConfirmModal';

export default function SupervisorPanel() {
  const [solicitudes, setSolicitudes] = useState([]);
  const [mensaje, setMensaje] = useState('');
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmData, setConfirmData] = useState(null);

  useEffect(() => {
    cargarSolicitudes();
  }, []);

  const cargarSolicitudes = async () => {
    try {
      const res = await api.get('/api/admin/pendientes');
      setSolicitudes(res.data);
    } catch (err) {
      console.error("Error al cargar solicitudes:", err);
    }
  };

  const manejarAccionAlbum = async (id, accion) => {
    // Mostrar modal de confirmación en lugar de confirm nativo
    setConfirmData({ id, accion });
    setConfirmOpen(true);
  };

  const ejecutarAccionAlbum = async () => {
    const { id, accion } = confirmData || {};
    setConfirmOpen(false);
    if (!id || !accion) return;

    try {
      await api.get('/api/auth/csrf');
      const res = accion === 'aprobar'
        ? await api.post(`/api/admin/albums/${id}/aprobar`)
        : await api.delete(`/api/admin/albums/${id}/rechazar`);

      mostrarMensaje(` ${res.data}`, 'success');
      cargarSolicitudes();
    } catch (err) {
      mostrarMensaje(` Error: ${err.response?.data || 'No se pudo completar la acción'}`, 'error');
    }
  };

  const manejarAccionImagen = async (id, accion) => {
    try {
      await api.get('/api/auth/csrf');
      const res = accion === 'aprobar'
        ? await api.put(`/api/admin/image/${id}/approve`)
        : await api.put(`/api/admin/image/${id}/reject`);
        
      mostrarMensaje(` ${res.data}`, 'success');
      cargarSolicitudes(); // Recargamos para ver los cambios en la cuadrícula
    } catch (err) {
      mostrarMensaje(` Error en imagen: ${err.response?.data || 'Error interno'}`, 'error');
    }
  };

  const mostrarMensaje = (texto, tipo) => {
    setMensaje({ texto, tipo });
    setTimeout(() => setMensaje(''), 5000);
  };

  return (
    <div className="supervisor-panel" style={{ padding: '20px', maxWidth: '1100px', margin: '0 auto' }}>
      <h2 style={{ borderBottom: '3px solid #2c3e50', paddingBottom: '10px', color: '#2c3e50' }}>
       Centro de Control y Auditoría Perimetral
      </h2>
      
      {mensaje && (
        <div style={{ 
          background: mensaje.tipo === 'error' ? '#f8d7da' : '#d4edda', 
          color: mensaje.tipo === 'error' ? '#721c24' : '#155724', 
          padding: '12px', borderRadius: '5px', marginBottom: '20px', fontWeight: 'bold' 
        }}>
          {mensaje.texto}
        </div>
      )}

      <ConfirmModal
        open={confirmOpen}
        title="Confirmar acción"
        message={`¿Estás seguro de que deseas ${confirmData?.accion || ''} este álbum completo?`}
        onConfirm={ejecutarAccionAlbum}
        onCancel={() => setConfirmOpen(false)}
        confirmText="Aceptar"
        cancelText="Cancelar"
      />

      {solicitudes.length === 0 ? (
        <p style={{ fontSize: '1.1em', color: '#666' }}> No hay solicitudes de álbumes pendientes de revisión.</p>
      ) : (
        solicitudes.map(album => {
          const tieneInfecciones = album.imagenes.some(img => img.estado === 'QUARANTINE');

          return (
            <div key={album.id} style={{ background: 'white', border: '1px solid #ddd', borderRadius: '8px', marginBottom: '30px', boxShadow: '0 4px 8px rgba(0,0,0,0.05)', overflow: 'hidden' }}>
              
              {/* Cabecera del Álbum */}
              <div style={{ background: tieneInfecciones ? '#fff3cd' : '#f8f9fa', padding: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #eee' }}>
                <div>
                  <h3 style={{ margin: '0 0 8px 0', color: '#333' }}> {album.titulo}</h3>
                  <p style={{ margin: 0, color: '#555' }}>{album.descripcion}</p>
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <button onClick={() => manejarAccionAlbum(album.id, 'aprobar')} style={{ background: '#2ec27e', color: 'white', border: 'none', padding: '10px 15px', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' }}>
                    Aprobar Álbum Completo
                  </button>
                  <button onClick={() => manejarAccionAlbum(album.id, 'rechazar')} style={{ background: '#e01b24', color: 'white', border: 'none', padding: '10px 15px', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' }}>
                    Rechazar y Destruir
                  </button>
                </div>
              </div>

              {tieneInfecciones && (
                <div style={{ background: '#f8d7da', color: '#721c24', padding: '12px 20px', fontWeight: 'bold', borderBottom: '1px solid #f5c6cb' }}>
                   ATENCIÓN: Se detectó archivos anómalos.
                </div>
              )}

              {/* Cuadrícula de Archivos */}
              <div style={{ padding: '20px', display: 'flex', flexWrap: 'wrap', gap: '20px', background: '#fafafa' }}>
                {album.imagenes.map(img => (
                  <div key={img.id} style={{ width: '200px', border: '1px solid #ddd', borderRadius: '6px', overflow: 'hidden', background: 'white', display: 'flex', flexDirection: 'column' }}>
                    
                    {/* Visualizador de la imagen */}
                    <div style={{ height: '140px', background: '#e9ecef', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
                      {img.estado === 'QUARANTINE' && (
                        <div style={{ 
                          padding: '12px', 
                          background: '#1e1e1e', // Fondo oscuro tipo terminal
                          borderTop: '2px solid #dc3545',
                          fontSize: '0.8em',
                          textAlign: 'left',
                          color: '#00ff00', // Texto verde tipo matrix/consola
                          fontFamily: 'monospace'
                        }}>
                          <strong style={{ color: '#ff4d4d' }}>[DETECCION_ANOMALIA_REPORT]</strong>
             
                          
                          <div style={{ marginBottom: '10px' }}>
                            <span style={{ color: '#ffc107' }}>{img.motivoAlerta || "Inconsistencia estructural genérica"}</span>
                          </div>
                          
                          <div style={{ borderTop: '1px solid #444', paddingTop: '8px' }}>
                            <ul style={{ listStyle: 'none', padding: 0, margin: '5px 0' }}>
                          
                              <li> ESTEGANOGRAFIA_LSB: <span style={{color: '#ff4d4d'}}>DETECTADA</span></li>
                              <li> DATA_POST_EOF: <span style={{color: '#ff4d4d'}}>TRUE</span></li>
                              <li> Name of the file: <span style={{color: '#ff4d4d'}}>{img.nombreArchivo}</span></li>
                            </ul>
                          </div>
                        </div>
                      )}
                    </div>
                    
                    {/* Estado de Seguridad */}
                    <div style={{ padding: '8px', textAlign: 'center', fontSize: '0.85em', fontWeight: 'bold', background: img.estado === 'QUARANTINE' ? '#dc3545' : '#28a745', color: 'white' }}>
                      {img.estado === 'QUARANTINE' ? ' CUARENTENA' : ' SEGURO'}
                    </div>

                    {/* Acciones Individuales (Solo si está en cuarentena) */}
                    {img.estado === 'QUARANTINE' && (
                      <div style={{ display: 'flex', padding: '10px', gap: '5px', background: '#f8f9fa' }}>
                         <button onClick={() => manejarAccionImagen(img.id, 'aprobar')} style={{ flex: 1, background: '#ffc107', color: '#000', border: 'none', padding: '5px', borderRadius: '3px', fontSize: '0.8em', cursor: 'pointer' }} title="Ignorar alerta y aprobar">
                           Aprobar
                         </button>
                         <button onClick={() => manejarAccionImagen(img.id, 'rechazar')} style={{ flex: 1, background: '#343a40', color: 'white', border: 'none', padding: '5px', borderRadius: '3px', fontSize: '0.8em', cursor: 'pointer' }} title="Destruir esta imagen">
                           Eliminar
                         </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>

            </div>
          );
        })
      )}
    </div>
  );
}