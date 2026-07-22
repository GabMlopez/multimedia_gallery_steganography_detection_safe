describe("Vista pública de álbum inocuo", () => {
  beforeEach(() => {
    cy.intercept("GET", "**/api/auth/csrf", {
      statusCode: 200,
      body: { token: "mock-csrf-token" }
    }).as("csrf");

    cy.intercept("GET", "**/api/albums/publico/todos", {
      statusCode: 200,
      body: [
        {
          id: 1,
          titulo: "Álbum seguro",
          descripcion: "Álbum con imágenes aprobadas y sin riesgo",
          estado: "APPROVED",
          imagenes: [
            {
              id: 101,
              nombreArchivo: "foto-1.png",
              estado: "CLEAN",
              rutaLocal: "/uploads/safe/foto-1.png"
            },
            {
              id: 102,
              nombreArchivo: "foto-2.png",
              estado: "CLEAN",
              rutaLocal: "/uploads/safe/foto-2.png"
            },
            {
              id: 103,
              nombreArchivo: "foto-3.png",
              estado: "CLEAN",
              rutaLocal: "/uploads/safe/foto-3.png"
            }
          ]
        }
      ]
    }).as("getPublicAlbums");

    cy.intercept("GET", "**/api/albums/publico/1", {
      statusCode: 200,
      body: {
        album: {
          id: 1,
          titulo: "Álbum seguro",
          descripcion: "Álbum con imágenes aprobadas y sin riesgo",
          estado: "APPROVED"
        },
        imagenes: [
          {
            id: 101,
            nombreArchivo: "foto-1.png",
            estado: "CLEAN",
            rutaLocal: "/uploads/safe/foto-1.png"
          },
          {
            id: 102,
            nombreArchivo: "foto-2.png",
            estado: "CLEAN",
            rutaLocal: "/uploads/safe/foto-2.png"
          },
          {
            id: 103,
            nombreArchivo: "foto-3.png",
            estado: "CLEAN",
            rutaLocal: "/uploads/safe/foto-3.png"
          }
        ]
      }
    }).as("getAlbumDetail");

    cy.visit("/", {
      onBeforeLoad(win) {
        win.localStorage.setItem("user", JSON.stringify({
          id: 1,
          username: "viewer",
          role: "ROLE_USER"
        }));
      }
    });

    cy.wait("@getPublicAlbums");
  });

  it("muestra la portada con el héroe y la colección pública", () => {
    cy.contains("Galería segura").should("be.visible");
    cy.contains("Explora álbumes públicos").should("be.visible");
    cy.contains("Ir a mi panel").should("be.visible");
    cy.contains("Sesión activa: viewer").should("be.visible");
    cy.contains("álbumes públicos").should("be.visible");
    cy.contains("imágenes visibles").should("be.visible");
    cy.contains("RBAC").should("be.visible");
  });

  it("renderiza la tarjeta del álbum público con su acción principal", () => {
    cy.contains("Álbum público").should("be.visible");
    cy.contains("Álbum seguro").should("be.visible");
    cy.contains("Álbum con imágenes aprobadas y sin riesgo").should("be.visible");
    cy.contains("Abrir álbum").should("be.visible");
  });

  it("abre el álbum público y muestra la galería de imágenes aprobadas", () => {
    cy.contains("Abrir álbum").click();
    cy.wait("@getAlbumDetail");

    cy.contains("Galería").should("be.visible");
    cy.contains("Álbum seguro").should("be.visible");
    cy.contains("foto-1.png").should("be.visible");
    cy.contains("foto-2.png").should("be.visible");
    cy.contains("foto-3.png").should("be.visible");
  });
});
