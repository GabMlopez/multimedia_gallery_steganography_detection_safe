import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, apiUrl } from '../lib/api'; 

export default function Album() {
  const { id } = useParams();
  const user = JSON.parse(localStorage.getItem('user')); // Leemos el usuario para el render condicional
  
  const [albumInfo, setAlbumInfo] = useState(null);
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [archivosExtra, setArchivosExtra] = useState([]);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadMessage, setUploadMessage] = useState('');

  // 1. Centralizamos la carga en una sola función que use la ruta PÚBLICA
  const cargarAlbum = () => {
    setLoading(true);
    // Llamada correcta al nuevo endpoint de detalle
    api.get(`/api/albums/publico/${id}`) 
      .then(res => {
        setAlbumInfo(res.data.album);
        setImages(res.data.imagenes);
      })
      .catch(err => {
        console.error("Error al cargar detalle:", err);
        setError("Álbum no encontrado o no aprobado.");
      })
      .finally(() => setLoading(false));
  };


  useEffect(() => {
    cargarAlbum();
  }, [id]);

  const subirArchivosAlAlbum = async (e) => {
    e.preventDefault();
    if (archivosExtra.length === 0) return;

    setIsUploading(true);
    setUploadMessage('');

    const formData = new FormData();
    for (let i = 0; i < archivosExtra.length; i++) {
      formData.append('archivos', archivosExtra[i]);
    }

    try {
      await api.get('/api/auth/csrf');
      const res = await api.post(`/api/images/upload/${id}`, formData);
      
      setUploadMessage(`✅ ${res.data}`);
      setArchivosExtra([]);
      if(document.getElementById('file-upload-album')) {
          document.getElementById('file-upload-album').value = null;
      }

      cargarAlbum(); // Recarga usando la ruta pública
      setTimeout(() => setUploadMessage(''), 5000);
    } catch (err) {
      setUploadMessage(`❌ Error: ${err.response?.data || 'No se pudieron subir los archivos.'}`);
    } finally {
      setIsUploading(false);
    }
  };

  if (loading && !albumInfo) return <div style={{ padding: '20px' }}>⏳ Cargando evidencias...</div>;
  if (error) return <div style={{ padding: '20px', color: '#721c24', background: '#f8d7da' }}>❌ {error}</div>;

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto', padding: '20px' }}>
      
      {albumInfo && (
        <div style={{ marginBottom: '20px', paddingBottom: '15px', borderBottom: '2px solid #eee' }}>
          <h2 style={{ margin: '0 0 10px 0', color: '#2c3e50' }}>📂 {albumInfo.titulo}</h2>
          <p style={{ margin: 0, color: '#555', fontSize: '1.1em' }}>{albumInfo.descripcion}</p>
        </div>
      )}

      <h3 style={{ borderBottom: '2px solid #eee', paddingBottom: '10px' }}>Galería</h3>
      
      <div className="gallery" style={{ display: 'flex', flexWrap: 'wrap', gap: '20px', marginTop: '20px' }}>
        {images.length === 0 ? (
          <p style={{ color: '#666', fontStyle: 'italic' }}>Este álbum aún no tiene imágenes aprobadas.</p>
        ) : (
          images.map(img => (
            <div className="image-card" key={img.id} style={{ width: '220px', border: '1px solid #ddd', borderRadius: '8px', overflow: 'hidden' }}>
              <div style={{ height: '160px', background: '#e9ecef', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                <img 
                  src={apiUrl(`/api/public/view/${img.nombreArchivo}`)} 
                  alt={img.nombreArchivo} 
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                />
              </div>
              <div className="meta" style={{ padding: '12px' }}>
                <p style={{ margin: '0 0 5px 0', fontSize: '0.85em', wordBreak: 'break-all' }}>📄 {img.nombreArchivo}</p>
                <span style={{ color: '#137333', fontSize: '0.75em', fontWeight: 'bold' }}>✅ Inspección aprobada</span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}