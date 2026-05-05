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

  const cargarAlbums = () => {
    api.get('/api/albums/todos')
      .then(res => setMisAlbums(Array.isArray(res.data) ? res.data : []))
      .catch(err => {
        console.error('Error cargando albumes:', err);
        setMisAlbums([]);
      });
  };

  // Cargar álbumes al iniciar para llenar el selector del Formulario 2
  useEffect(() => {
    cargarAlbums();
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
      setMessage("✅ Éxito: " + res.data + " (si queda en revisión, aparecerá como destino cuando sea aprobado)");
      
      // Limpiar formulario 1
      setTitulo(''); setDescripcion(''); setArchivosLote([]);
      document.getElementById('input-lote').value = null;
      cargarAlbums();
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

  const hasMessageError = message.includes('Error') || message.includes('⚠️') || message.includes('❌');

  return (
    <div className="user-panel">
      <section className="user-panel__hero">
        <div>
          <span className="login-card__eyebrow">Área privada</span>
          <h2>Panel de Usuario</h2>
          <p>Administra tus álbumes, sube imágenes y organiza tu contenido en un espacio seguro.</p>
        </div>
        <div className="user-panel__hero-badges">
          <div>
            <strong>2 pasos</strong>
            <span>crear o subir</span>
          </div>
          <div>
            <strong>Multiarchivo</strong>
            <span>carga en lote</span>
          </div>
          <div>
            <strong>Protegido</strong>
            <span>con sesión activa</span>
          </div>
        </div>
      </section>

      {message && (
        <div className={`message user-panel__message ${hasMessageError ? 'is-error' : 'is-success'}`}>
          {message}
        </div>
      )}

      <div className="user-panel__grid">
        
        {/* FORMULARIO 1: Lote */}
        <section className="album user-panel__card">
          <div className="user-panel__card-header">
            <span className="user-panel__step">Paso 1</span>
            <h3>Crear nuevo álbum</h3>
            <p>Crea un álbum y opcionalmente adjunta múltiples imágenes.</p>
          </div>

          <form className="user-panel__form" onSubmit={crearAlbumEnLote}>
            <label className="field">
              <span>Título del álbum</span>
              <div className="field__control">
                <input 
                  type="text" placeholder="Ej: Viaje a la costa" required
                  value={titulo} onChange={e => setTitulo(e.target.value)} 
                />
              </div>
            </label>

            <label className="field">
              <span>Descripción</span>
              <div className="field__control field__control--textarea">
                <textarea 
                  placeholder="Describe el contenido del álbum" required rows="4"
                  value={descripcion} onChange={e => setDescripcion(e.target.value)} 
                />
              </div>
            </label>

            <label className="user-panel__file-label">Archivos (Imágenes)</label>
            <div className="user-panel__file-box">
              <input 
                id="input-lote" type="file" multiple 
                accept="image/*,application/pdf"
                onChange={e => setArchivosLote(e.target.files)} 
              />
            </div>

            <button type="submit" className="user-panel__button user-panel__button--primary" disabled={isSubmittingLote}>
              {isSubmittingLote ? 'Procesando...' : 'Crear y Enviar a Revisión'}
            </button>
          </form>
        </section>

        <section className="album user-panel__card">
          <div className="user-panel__card-header">
            <span className="user-panel__step user-panel__step--alt">Paso 2</span>
            <h3>Agregar a álbum existente</h3>
            <p>Sube múltiples imágenes a un álbum ya creado y aprobado.</p>
            <button
              type="button"
              className="user-panel__button user-panel__button--secondary user-panel__button--small"
              style={{ marginTop: '0.5rem' }}
              onClick={cargarAlbums}
              title="Recargar lista de álbumes aprobados"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" style={{ verticalAlign: 'middle', marginRight: '8px' }}>
                <path fill="currentColor" d="M21 12a9 9 0 1 0-2.64 6.03l1.46-1.46A7 7 0 1 1 19 12h2z" />
              </svg>
              Recargar álbumes
            </button>
          </div>

          <form className="user-panel__form" onSubmit={subirArchivoExtra}>
            <label className="field">
              <span>Álbum destino</span>
              <div className="field__control">
                <select
                  value={albumId}
                  onChange={e => setAlbumId(e.target.value)}
                  required
                >
                  <option value="">-- Selecciona un álbum destino --</option>
                  {misAlbums.map(a => (
                    <option key={a.id} value={a.id}>{a.titulo || a.nombre || `Álbum ${a.id}`}</option>
                  ))}
                </select>
              </div>
              {misAlbums.length === 0 && (
                <small style={{ color: '#64748b' }}>
                  No hay álbumes aprobados disponibles todavía. Si acabas de crear uno, primero debe aprobarlo el supervisor.
                </small>
              )}
            </label>

            <label className="user-panel__file-label">Archivos (Imágenes)</label>
            <div className="user-panel__file-box">
              <input
                id="input-extra"
                type="file"
                multiple
                accept="image/*,application/pdf"
                onChange={e => setArchivosExtra(e.target.files)}
                required
              />
            </div>

            <button type="submit" className="user-panel__button user-panel__button--success" disabled={isSubmittingExtra}>
              {isSubmittingExtra ? 'Subiendo Lote...' : 'Subir Archivos'}
            </button>
          </form>
        </section>

      </div>
    </div>
  );
}