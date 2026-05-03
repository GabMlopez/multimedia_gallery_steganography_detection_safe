import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../lib/api'

export default function Home() {
  const [albums, setAlbums] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/albums/publico/todos') 
    .then(res => setAlbums(res.data))
    .catch(err => console.error(err))
    .finally(() => setLoading(false))
  }, [])

  if (loading) return <div>Loading albums...</div>

  return (
    <div>
      <h2>Álbumes públicos</h2>
      <div className="albums">
        {albums.map(album => (
          <div className="album" key={album.id}>
            <h3>{album.title || album.nombre || `Album ${album.id}`}</h3>
            <p>{album.description || ''}</p>
            <Link to={`/album/${album.id}`}>Ver imágenes</Link>
          </div>
        ))}
      </div>
    </div>
  )
}
