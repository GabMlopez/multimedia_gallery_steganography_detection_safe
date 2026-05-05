import React from 'react';

export default function ConfirmModal({ open, title, message, onConfirm, onCancel, confirmText = 'Aceptar', cancelText = 'Cancelar' }) {
  if (!open) return null;

  return (
    <div className="cm-backdrop" onClick={onCancel}>
      <div className="cm-modal" onClick={e => e.stopPropagation()} role="dialog" aria-modal="true">
        <header className="cm-header">
          <h3>{title}</h3>
        </header>
        <div className="cm-body">{message}</div>
        <footer className="cm-footer">
          <button className="cm-btn cm-btn--confirm" onClick={onConfirm}>{confirmText}</button>
          <button className="cm-btn cm-btn--cancel" onClick={onCancel}>{cancelText}</button>
        </footer>
      </div>
    </div>
  );
}
