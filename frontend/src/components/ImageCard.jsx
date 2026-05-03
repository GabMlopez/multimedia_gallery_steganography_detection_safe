import React from 'react'

export default function ImageCard({ img }) {
  return (
    <div className="image-card">
      <img src={`/api/public/view/${img.filename}`} alt={img.title || img.filename} />
      <div className="meta">{img.title}</div>
    </div>
  )
}
