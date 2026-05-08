import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../lib/api'

export default function Home() {
  const [albums, setAlbums] = useState([])
  const [loading, setLoading] = useState(true)
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('user'))
    } catch {
      return null
    }
  })

  useEffect(() => {
    api.get('/api/albums/publico/todos')
      .then(res => setAlbums(Array.isArray(res.data) ? res.data : []))
      .catch(err => console.error(err))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    const handleStorage = () => {
      try {
        setUser(JSON.parse(localStorage.getItem('user')))
      } catch {
        setUser(null)
      }
    }

    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [])

  if (loading) {
    return (
      <section className="loading-state hero-card">
        <span className="eyebrow">Cargando colección</span>
        <h2>Preparando álbumes públicos</h2>
        <p>Estamos trayendo el contenido disponible para que puedas explorarlo con rapidez.</p>
      </section>
    )
  }

  const totalImages = albums.reduce((count, album) => count + (Array.isArray(album.imagenes) ? album.imagenes.length : 0), 0)

  return (
    <div className="page page-home">
      <section className="page-hero hero-card">
        <div className="page-hero__copy">
          <span className="eyebrow">Galería segura</span>
          <h2>Explora álbumes públicos</h2>
          <p>
            Navega colecciones, abre álbumes aprobados y entra al sistema sin perderte en pantallas recargadas.
          </p>
          {user ? (
            <div className="page-actions">
              <Link
                className="button button--primary"
                to={user.role === 'ROLE_SUPERVISOR' ? '/supervisor' : '/user-panel'}
              >
                Ir a mi panel
              </Link>
              <span className="session-chip">Sesión activa: {user.username}</span>
            </div>
          ) : (
            <div className="page-actions">
              <Link className="button button--primary" to="/register">Crear cuenta</Link>
              <Link className="button button--ghost" to="/login">Acceder</Link>
            </div>
          )}
        </div>

        <div className="page-hero__stats">
          <article className="stat-card">
            <strong>{albums.length}</strong>
            <span>álbumes públicos</span>
          </article>
          <article className="stat-card">
            <strong>{totalImages}</strong>
            <span>imágenes visibles</span>
          </article>
          <article className="stat-card">
            <strong>RBAC</strong>
            <span>roles y acceso</span>
          </article>
        </div>
      </section>

      {albums.length === 0 ? (
        <section className="empty-state">
          <h3>No hay álbumes públicos por ahora</h3>
          <p>Cuando existan álbumes aprobados, aparecerán aquí para consulta.</p>
        </section>
      ) : (
        <section className="album-grid">
          {albums.map(album => (
            <article className="album-card" key={album.id}>
              <span className="album-card__kicker">Álbum público</span>
              <h3>{album.titulo || album.title || album.nombre || `Álbum ${album.id}`}</h3>
              <p>{album.descripcion || album.description || 'Sin descripción disponible.'}</p>
              <div className="album-card__footer">
                <span>{Array.isArray(album.imagenes) ? `${album.imagenes.length} imágenes` : 'Colección disponible'}</span>
                <Link to={`/album/${album.id}`} className="album-card__link">Abrir álbum</Link>
              </div>
            </article>
          ))}
        </section>
      )}
    </div>
  )
}
