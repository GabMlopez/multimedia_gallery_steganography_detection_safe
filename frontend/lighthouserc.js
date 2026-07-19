module.exports = {
  ci: {
    collect: {
      startServerCommand: "npm run preview",
      startServerReadyPattern: "Local:",
      url: ["http://localhost:4173/supervisor"],
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
        "tap-targets": "warn"
      }
    },
    upload: {
      target: "filesystem",
      outputDir: "lighthouse-reports"
    }
  }
};
