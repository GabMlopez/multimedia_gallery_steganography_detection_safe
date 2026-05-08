export function FormCard({ stepNumber, title, description, children }) {
  return (
    <section style={{
      background: 'linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(244, 250, 252, 0.96) 100%)',
      border: '1px solid rgba(20, 184, 166, 0.14)',
      borderRadius: '20px',
      padding: '24px',
      boxShadow: '0 18px 36px rgba(15, 23, 42, 0.08)',
      flex: '1',
      minWidth: '320px'
    }}>
      <div style={{ marginBottom: '20px' }}>
        <span style={{
          display: 'inline-block',
          backgroundColor: 'rgba(20, 184, 166, 0.12)',
          color: '#0f766e',
          padding: '4px 12px',
          borderRadius: '20px',
          fontSize: '0.75em',
          fontWeight: 'bold',
          marginBottom: '8px'
        }}>
          Paso {stepNumber}
        </span>
        <h3 style={{
          margin: '8px 0 4px 0',
          fontSize: '1.25em',
          fontWeight: 'bold',
          color: 'var(--text)'
        }}>
          {title}
        </h3>
        <p style={{
          margin: 0,
          fontSize: '0.9em',
          color: 'var(--muted)'
        }}>
          {description}
        </p>
      </div>
      
      {children}
    </section>
  );
}

export function StatsSection() {
  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
      gap: '16px',
      marginBottom: '32px'
    }}>
      {[
        { label: '2 Pasos', desc: 'crear o subir' },
        { label: 'Multiarchivo', desc: 'carga en lote' },
        { label: 'Protegido', desc: 'con sesión activa' }
      ].map((stat, idx) => (
        <div key={idx} style={{
          background: 'rgba(20, 184, 166, 0.08)',
          border: '1px solid rgba(20, 184, 166, 0.18)',
          borderRadius: '16px',
          padding: '16px',
          textAlign: 'center'
        }}>
          <div style={{
            fontSize: '1.1em',
            fontWeight: 'bold',
            color: '#0f766e',
            marginBottom: '4px'
          }}>
            {stat.label}
          </div>
          <div style={{
            fontSize: '0.8em',
            color: 'var(--muted)'
          }}>
            {stat.desc}
          </div>
        </div>
      ))}
    </div>
  );
}
