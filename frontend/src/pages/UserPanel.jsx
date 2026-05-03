import React, { useState, useEffect } from 'react';
import { api } from '../lib/api';

export default function UserPanel() {
  // --- Estados para Formulario 1: Crear Álbum en Lote ---
  const [titulo, setTitulo] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [archivosLote, setArchivosLote] = useState([]);
  const [isSubmittingLote, setIsSubmittingLote] = useState(false);

  const [albumId, setAlbumId] = useState('');
  const [archivosExtra, setArchivosExtra] = useState([]); // AHORA ES UN ARREGLO
  const [misAlbums, setMisAlbums] = useState([]);
  const [isSubmittingExtra, setIsSubmittingExtra] = useState(false);

  // Mensajes de sistema
  const [message, setMessage] = useState('');

  // Cargar álbumes al iniciar para llenar el selector del Formulario 2
  useEffect(() => {
    api.get('/api/public/albums')
      .then(res => setMisAlbums(res.data))
      .catch(err => console.error("Error cargando álbumes:", err));
  }, []);

  // --- Función 1: Crear Álbum (con o sin archivos) ---
  const crearAlbumEnLote = async (e) => {
    e.preventDefault();
    setIsSubmittingLote(true);
    setMessage('');
    
    const formData = new FormData();
    formData.append('titulo', titulo);
    formData.append('descripcion', descripcion);
    
    if (archivosLote.length > 0) {
      for (let i = 0; i < archivosLote.length; i++) {
        formData.append('archivos', archivosLote[i]);
      }
    }

    try {
      await api.get('/api/auth/csrf');
      const res = await api.post('/api/albums/solicitar-lote', formData);
      setMessage("✅ Éxito: " + res.data);
      
      // Limpiar formulario 1
      setTitulo(''); setDescripcion(''); setArchivosLote([]);
      document.getElementById('input-lote').value = null;
    } catch (err) {
      setMessage("❌ Error al crear álbum: " + (err.response?.data || "Error de red"));
    } finally {
      setIsSubmittingLote(false);
    }
  };

  const subirArchivoExtra = async (e) => {
    e.preventDefault();
    if (archivosExtra.length === 0 || !albumId) {
      setMessage("⚠️ Selecciona un álbum y al menos un archivo");
      return;
    }

    setIsSubmittingExtra(true);
    setMessage('');

    const formData = new FormData();
    // Iteramos para agregar todos los archivos seleccionados
    for (let i = 0; i < archivosExtra.length; i++) {
      formData.append("archivos", archivosExtra[i]); // La llave ahora es "archivos"
    }

    try {
      await api.get('/api/auth/csrf');
      const res = await api.post(`/api/images/upload/${albumId}`, formData);
      setMessage("✅ " + res.data);
      
      // Limpiar formulario 2
      setArchivosExtra([]);
      document.getElementById('input-extra').value = null;
    } catch (err) {
      const errorMsg = err.response?.data || "Error al subir los archivos";
      setMessage("❌ Error: " + errorMsg);
    } finally {
      setIsSubmittingExtra(false);
    }
  };
  return (
    <div className="user-panel" style={{ maxWidth: '900px', margin: '0 auto', padding: '20px' }}>
      <h2>Panel de Usuario</h2>
      
      {message && (
        <div className="message" style={{ background: message.includes('Error') || message.includes('⚠️') ? '#f8d7da' : '#d4edda', color: message.includes('Error') || message.includes('⚠️') ? '#721c24' : '#155724', padding: '10px', borderRadius: '5px', marginBottom: '20px' }}>
          {message}
        </div>
      )}

      {/* Contenedor Flex para poner los formularios lado a lado en pantallas grandes */}
      <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
        
        {/* FORMULARIO 1: Lote */}
        <div className="album" style={{ flex: '1 1 400px', background: 'white', padding: '20px', borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}>
          <h3>1. Crear Nuevo Álbum</h3>
          <p style={{ fontSize: '0.85em', color: '#666' }}>Crea un álbum y opcionalmente adjunta múltiples imágenes </p>

          <form onSubmit={crearAlbumEnLote} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
            <input 
              type="text" placeholder="Título del álbum" required
              value={titulo} onChange={e => setTitulo(e.target.value)} 
              style={{ padding: '8px', border: '1px solid #ccc', borderRadius: '4px' }}
            />
            <textarea 
              placeholder="Descripción" required rows="3"
              value={descripcion} onChange={e => setDescripcion(e.target.value)} 
              style={{ padding: '8px', border: '1px solid #ccc', borderRadius: '4px' }}
            />
            <label style={{ fontSize: '0.9em', fontWeight: 'bold' }}>Archivos (Imágenes):</label>
            <input 
              id="input-lote" type="file" multiple 
              accept="image/*,application/pdf" // Permite seleccionar imágenes o documentos
              onChange={e => setArchivosLote(e.target.files)} 
            />
            <button type="submit" disabled={isSubmittingLote} style={{ background: isSubmittingLote ? '#95a5a6' : '#1a5fb4', color: 'white', padding: '10px', border: 'none', borderRadius: '4px' }}>
              {isSubmittingLote ? 'Procesando...' : 'Crear y Enviar a Revisión'}
            </button>
          </form>
        </div>

        <div className="album" style={{ flex: '1 1 400px', background: 'white', padding: '20px', borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}>
          <h3>2. Agregar a Álbum Existente</h3>
          <p style={{ fontSize: '0.85em', color: '#666' }}>Sube múltiples imágenes a un álbum ya creado.</p>

          <form onSubmit={subirArchivoExtra} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
            <select 
              value={albumId} onChange={e => setAlbumId(e.target.value)} required
              style={{ padding: '8px', border: '1px solid #ccc', borderRadius: '4px' }}
            >
              <option value="">-- Selecciona un álbum destino --</option>
              {misAlbums.map(a => (
                <option key={a.id} value={a.id}>{a.titulo || a.nombre || `Álbum ${a.id}`}</option>
              ))}
            </select>
            
            <label style={{ fontSize: '0.9em', fontWeight: 'bold' }}>Archivos (Imágenes):</label>
            <input 
              id="input-extra" 
              type="file" 
              multiple // ¡ESTA ES LA MAGIA VISUAL!
              accept="image/*,application/pdf" 
              onChange={e => setArchivosExtra(e.target.files)} // Guardamos el FileList completo
              required
            />
            <button type="submit" disabled={isSubmittingExtra} style={{ background: isSubmittingExtra ? '#95a5a6' : '#2ec27e', color: 'white', padding: '10px', border: 'none', borderRadius: '4px' }}>
              {isSubmittingExtra ? 'Subiendo Lote...' : 'Subir Archivos'}
            </button>
          </form>
        </div>

      </div>
    </div>
  );
}