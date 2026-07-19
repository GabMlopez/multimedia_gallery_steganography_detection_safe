module.exports = {
  ci: {
    collect: {
      startServerCommand: "npx vite preview --port 3001",
      startServerReadyPattern: "Local:",
      url: ["http://localhost:3001/supervisor"],
      puppeteerScript: "./lighthouse-auth.js",
      numberOfRuns: 3,
      settings: {
        preset: "desktop",
        throttlingMethod: "simulate",
        onlyCategories: [
          "performance",
          "accessibility",
          "best-practices",
          "seo"
        ]
      }
    },
    assert: {
      preset: "lighthouse:no-pwa",
      assertions: {
        "categories:performance": ["warn", { minScore: 0.6 }],
        "categories:accessibility": ["error", { minScore: 0.8 }],
        "categories:best-practices": ["error", { minScore: 0.8 }],
        "categories:seo": ["warn", { minScore: 0.8 }],
        "color-contrast": "warn",
        "html-has-lang": "error",
        "image-alt": "warn",
        "meta-description": "warn",
        "errors-in-console": "warn",
        "unused-css-rules": "warn",
        "unused-javascript": "warn",
        "lcp-lazy-loaded": "off",
        "non-composited-animations": "off",
        "prioritize-lcp-image": "off",
        "robots-txt": "off",
        "tap-targets": "off"
      }
    },
    upload: {
      target: "filesystem",
      outputDir: "lighthouse-reports"
    }
  }
};
