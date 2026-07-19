describe("Modulo Cuarentena - Contenido Oculto Danino", () => {
  beforeEach(() => {
    cy.intercept("GET", "**/api/auth/csrf", {
      statusCode: 200,
      body: { token: "mock-csrf-token" }
    }).as("csrf");
    cy.intercept("GET", "**/api/admin/pendientes", {
      statusCode: 200,
      body: [{
        id: 1,
        titulo: "Album sospechoso",
        descripcion: "Album con contenido anomalo",
        imagenes: [{
          id: 10,
          nombreArchivo: "maliciosa.png",
          estado: "QUARANTINE",
          motivoAlerta: "Anomalia estructural detectada"
        }]
      }]
    }).as("getPendientes");
    cy.intercept("PUT", "**/api/admin/image/*/reject", {
      statusCode: 200,
      body: "Imagen rechazada y eliminada de los servidores."
    }).as("rejectImage");
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

  it("Muestra alerta de archivos anomalos en el lote", () => {
    cy.contains("ATENCIÓN: Se detectó archivos anómalos en este lote.").should("be.visible");
  });

  it("Muestra el indicador de riesgo alto (badge rojo de CUARENTENA)", () => {
    cy.contains("CUARENTENA")
      .should("be.visible")
      .and("have.css", "background-color", "rgb(239, 68, 68)");
  });

  it("Muestra el detalle de la alerta con el motivo (ej: script ejecutable)", () => {
    cy.get("[data-cy=quarantine-motivo]")
      .should("be.visible")
      .and("contain", "Anomalia");
  });

  it("NO muestra la imagen peligrosa como preview (solo texto forense)", () => {
    cy.get("img").should("have.length", 0);
    cy.contains("[DETECCION_ANOMALIA]").should("be.visible");
  });

  it("Muestra el mensaje recomendando eliminar el archivo danino", () => {
    cy.contains("button", "Borrar")
      .should("be.visible")
      .and("contain", "Borrar");
  });

  it("Boton Borrar (reject) elimina la imagen danina permanentemente", () => {
    cy.contains("button", "Borrar").click();
    cy.wait("@rejectImage").its("response.statusCode").should("be.oneOf", [200, 404]);
  });

  it("Muestra notificacion de eliminacion al rechazar contenido danino", () => {
    cy.contains("button", "Borrar").click();
    cy.wait("@rejectImage");
    cy.contains("eliminada", { timeout: 5000 }).should("be.visible");
  });

  it("El flujo completo: rechazar imagen danina en cuarentena", () => {
    cy.contains("button", "Borrar").click();
    cy.wait("@rejectImage").its("response.statusCode").should("eq", 200);
    cy.contains("Imagen rechazada y eliminada de los servidores.").should("be.visible");
    cy.contains("eliminada de los servidores").should("be.visible");
  });

  it("Muestra los hallazgos tecnicos de esteganografia", () => {
    cy.contains("ESTEGANOGRAFIA_LSB:").should("be.visible");
    cy.contains("DATA_POST_EOF:").should("be.visible");
    cy.contains("Name of the file:").should("be.visible");
  });
});
