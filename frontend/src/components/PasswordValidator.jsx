export function PasswordValidator({ password }) {
  const hasUpperCase = /[A-Z]/.test(password);
  const hasSpecialChar = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password);
  const hasMinLength = password.length >= 8;
  const isValid = hasUpperCase && hasSpecialChar && hasMinLength;

  const requirements = [
    { label: 'Al menos 8 caracteres', met: hasMinLength },
    { label: 'Letra mayúscula (A-Z)', met: hasUpperCase },
    { label: 'Carácter especial (!@#$%...)', met: hasSpecialChar }
  ];

  return (
    <div style={{
      marginTop: '12px',
      padding: '12px',
      backgroundColor: 'rgba(255, 255, 255, 0.92)',
      borderRadius: '14px',
      border: `1px solid ${isValid ? 'rgba(20, 184, 166, 0.35)' : 'rgba(15, 23, 42, 0.1)'}`,
      boxShadow: '0 12px 24px rgba(15, 23, 42, 0.04)'
    }}>
      <div style={{
        fontSize: '0.85em',
        fontWeight: 'bold',
        marginBottom: '8px',
        color: isValid ? '#0f766e' : 'var(--muted-strong)'
      }}>
        {isValid ? '✓ Contraseña válida' : 'Requisitos de contraseña'}
      </div>
      
      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
        {requirements.map((req, idx) => (
          <div
            key={idx}
            style={{
              display: 'flex',
              alignItems: 'center',
              fontSize: '0.8em',
              color: req.met ? '#0f766e' : 'var(--muted)'
            }}
          >
            <span style={{
              display: 'inline-block',
              width: '16px',
              height: '16px',
              marginRight: '8px',
              borderRadius: '3px',
                backgroundColor: req.met ? '#14b8a6' : '#dbe7ef',
              color: 'white',
              textAlign: 'center',
              lineHeight: '16px',
              fontSize: '0.7em',
              fontWeight: 'bold'
            }}>
              {req.met ? '✓' : ''}
            </span>
            {req.label}
          </div>
        ))}
      </div>
    </div>
  );
}

export function isPasswordValid(password) {
  const hasUpperCase = /[A-Z]/.test(password);
  const hasSpecialChar = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password);
  const hasMinLength = password.length >= 8;
  return hasUpperCase && hasSpecialChar && hasMinLength;
}
