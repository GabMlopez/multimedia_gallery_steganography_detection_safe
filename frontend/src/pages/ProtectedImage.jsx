import React, { useState, useEffect } from 'react';
import { api } from '../lib/api';

export default function ProtectedImage({ url, alt, className }) {
  const [imgSrc, setImgSrc] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    // Usamos Axios para enviar las cookies de sesión automáticamente
    api.get(url, { responseType: 'blob' })
      .then(response => {
        // Convertimos los bytes seguros en una URL local temporal que React puede mostrar
        const blobUrl = URL.createObjectURL(response.data);
        setImgSrc(blobUrl);
      })
      .catch(err => {
        console.error("Acceso denegado a la imagen protegida:", err);
        setError(true);
      });
  }, [url]);

  if (error) return <img src="/placeholder.png" alt="Error de seguridad" className={className} />;
  if (!imgSrc) return <span>Cargando imagen segura...</span>;

  return <img src={imgSrc} alt={alt} className={className} />;
}