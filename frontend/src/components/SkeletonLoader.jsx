export function SkeletonCard() {
  return (
    <div style={{
      backgroundColor: 'rgba(255, 255, 255, 0.88)',
      border: '1px solid rgba(15, 23, 42, 0.08)',
      borderRadius: '18px',
      padding: '12px',
      animation: 'pulse 2s ease-in-out infinite',
      boxShadow: '0 14px 28px rgba(15, 23, 42, 0.05)'
    }}>
      <div style={{
        width: '100%',
        height: '200px',
        backgroundColor: '#dce8ef',
        borderRadius: '14px',
        marginBottom: '12px'
      }} />
      <div style={{
        width: '80%',
        height: '16px',
        backgroundColor: '#dce8ef',
        borderRadius: '999px',
        marginBottom: '8px'
      }} />
      <div style={{
        width: '60%',
        height: '12px',
        backgroundColor: '#dce8ef',
        borderRadius: '999px'
      }} />
    </div>
  );
}

export function SkeletonText({ width = '100%', height = '16px', lines = 1 }) {
  return (
    <div style={{ marginBottom: '8px' }}>
      {Array.from({ length: lines }).map((_, i) => (
        <div
          key={i}
          style={{
            width: i === lines - 1 ? '70%' : width,
            height,
            backgroundColor: '#dce8ef',
            borderRadius: '999px',
            marginBottom: i < lines - 1 ? '8px' : '0',
            animation: 'pulse 2s ease-in-out infinite'
          }}
        />
      ))}
    </div>
  );
}

export const skeletonStyles = `
  @keyframes pulse {
    0%, 100% {
      opacity: 1;
    }
    50% {
      opacity: 0.5;
    }
  }
`;
