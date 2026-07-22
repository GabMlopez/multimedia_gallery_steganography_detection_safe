describe("Vista pública de álbum con imágenes no autorizadas - Home", () => {
  beforeEach(() => {
    cy.intercept("GET", "**/api/auth/csrf", {
      statusCode: 200,
      body: { token: "mock-csrf-token" }
    }).as("csrf");

    cy.intercept("GET", "**/api/albums/publico/todos", {
      statusCode: 200,
      body: [
        {
          id: 2,
          titulo: "Álbum con imágenes no autorizadas",
          descripcion: "Álbum que contiene imágenes rechazadas por seguridad",
          estado: "UNDER_REVIEW",
          imagenes: [
            {
              id: 201,
              nombreArchivo: "foto-harmful-1.png",
              estado: "QUARANTINE",
              rutaLocal: "/uploads/quarantine/foto-harmful-1.png"
            },
            {
              id: 202,
              nombreArchivo: "foto-harmful-2.png",
              estado: "HARMFUL",
              rutaLocal: "/uploads/quarantine/foto-harmful-2.png"
            }
          ]
        }
      ]
    }).as("getPublicAlbums");

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

  it("muestra el álbum público en la home con su resumen", () => {
    cy.contains("Galería segura").should("be.visible");
    cy.contains("Explora álbumes públicos").should("be.visible");
    cy.contains("Álbum público").should("be.visible");
    cy.contains("Álbum con imágenes no autorizadas").should("be.visible");
    cy.contains("Álbum que contiene imágenes rechazadas por seguridad").should("be.visible");
    cy.contains("2 imágenes").should("be.visible");
  });
});

describe("Vista pública de álbum con imágenes no autorizadas - Detalle", () => {
  beforeEach(() => {
    cy.intercept("GET", "**/api/auth/csrf", {
      statusCode: 200,
      body: { token: "mock-csrf-token" }
    }).as("csrf");

    cy.intercept("GET", "**/api/albums/publico/todos", {
      statusCode: 200,
      body: [
        {
          id: 2,
          titulo: "Álbum con imágenes no autorizadas",
          descripcion: "Álbum que contiene imágenes rechazadas por seguridad",
          estado: "UNDER_REVIEW",
          imagenes: [
            {
              id: 201,
              nombreArchivo: "foto-harmful-1.png",
              estado: "QUARANTINE",
              rutaLocal: "/uploads/quarantine/foto-harmful-1.png"
            },
            {
              id: 202,
              nombreArchivo: "foto-harmful-2.png",
              estado: "HARMFUL",
              rutaLocal: "/uploads/quarantine/foto-harmful-2.png"
            }
          ]
        }
      ]
    }).as("getPublicAlbums");

    cy.intercept("GET", "**/api/albums/publico/2", {
      statusCode: 200,
      body: {
        album: {
          id: 2,
          titulo: "Álbum con imágenes no autorizadas",
          descripcion: "Álbum que contiene imágenes rechazadas por seguridad",
          estado: "UNDER_REVIEW"
        },
        imagenes: []
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

  it("abre el álbum sin listar imágenes no autorizadas", () => {
    cy.contains("Abrir álbum").click();
    cy.wait("@getAlbumDetail");

    cy.contains("Galería").should("be.visible");
    cy.contains("Álbum con imágenes no autorizadas").should("be.visible");
    cy.contains("Este álbum aún no tiene imágenes aprobadas.").should("be.visible");
    cy.contains("foto-harmful-1.png").should("not.exist");
    cy.contains("foto-harmful-2.png").should("not.exist");
  });
});
