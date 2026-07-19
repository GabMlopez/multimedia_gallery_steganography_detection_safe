describe("Modulo Cuarentena - Contenido Oculto No Danino", () => {
  beforeEach(() => {
    cy.intercept("GET", "**/api/auth/csrf", {
      statusCode: 200,
      body: { token: "mock-csrf-token" }
    }).as("csrf");
    cy.intercept("GET", "**/api/admin/pendientes", {
      statusCode: 200,
      body: [{
        id: 1,
        titulo: "Album en revision",
        descripcion: "Album pendiente de revision",
        imagenes: [{
          id: 10,
          nombreArchivo: "imagen-segura.png",
          estado: "QUARANTINE",
          motivoAlerta: "Contenido potencialmente no danino"
        }]
      }]
    }).as("getPendientes");
    cy.intercept("PUT", "**/api/admin/image/*/approve", {
      statusCode: 200,
      body: "Imagen aprobada y enviada a la bóveda segura."
    }).as("approveImage");
    cy.visit("/supervisor", {
      onBeforeLoad(win) {
        win.localStorage.setItem('user', JSON.stringify({
          id: 1,
          username: 'supervisor',
          role: 'ROLE_SUPERVISOR'
        }));
      }
    });
    cy.wait("@getPendientes");
  });

  it("Muestra el panel de cuarentena correctamente", () => {
    cy.contains("Centro de Control y Auditoría Perimetral").should("be.visible");
  });

  it("Muestra album con imagenes en cuarentena", () => {
    cy.contains("ATENCIÓN: Se detectó archivos anómalos en este lote.").should("be.visible");
  });

  it("Muestra el detalle de deteccion de esteganografia para el archivo en cuarentena", () => {
    cy.contains("[DETECCION_ANOMALIA]").should("be.visible");
    cy.contains("ESTEGANOGRAFIA_LSB").should("be.visible");
    cy.contains("DATA_POST_EOF").should("be.visible");
  });

  it("Muestra la lista de hallazgos del analisis (motivoAlerta)", () => {
    cy.get("[data-cy=quarantine-motivo]").should("be.visible");
  });

  it("Muestra el badge de CUARENTENA en la imagen analizada", () => {
    cy.contains("CUARENTENA").should("be.visible");
  });

  it("Habilita boton Forzar (approve) para liberar archivo de cuarentena", () => {
    cy.contains("button", "Forzar").should("be.visible").click();
    cy.wait("@approveImage").its("response.statusCode").should("be.oneOf", [200, 404]);
  });

  it("Despues de aprobar, muestra pregunta de guardar en album (lo maneja el backend)", () => {
    cy.contains("button", "Forzar").click();
    cy.wait("@approveImage");
    cy.contains("Imagen aprobada", { timeout: 5000 }).should("be.visible");
  });

  it("Muestra notificacion de exito al aprobar una imagen", () => {
    cy.contains("button", "Forzar").click();
    cy.contains("Imagen aprobada", { timeout: 5000 }).should("be.visible");
  });

  it("El flujo completo: aprobar imagen en cuarentena no danina", () => {
    cy.contains("button", "Forzar").click();
    cy.wait("@approveImage").its("response.statusCode").should("eq", 200);
    cy.contains("Imagen aprobada y enviada a la bóveda segura.").should("be.visible");
    cy.contains("bóveda segura").should("be.visible");
  });
});
