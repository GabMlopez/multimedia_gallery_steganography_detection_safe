import React from 'react'
import { apiUrl } from '../lib/api'

export default function ImageCard({ img }) {
  const filename = img?.nombreArchivo || img?.filename || ''
  const title = img?.titulo || img?.title || filename

  return (
    <article className="image-card">
      <div className="image-card__preview">
        <img src={apiUrl(`/api/public/view/${filename}`)} alt={title} />
      </div>
      <div className="image-card__meta">
        <strong>{title}</strong>
        <span>{filename}</span>
      </div>
    </article>
  )
}
