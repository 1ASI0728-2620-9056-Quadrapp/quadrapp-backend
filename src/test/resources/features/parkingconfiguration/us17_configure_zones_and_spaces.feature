# language: es
@US17 @EP04
Característica: Configurar zonas y espacios
  Como administrador de estacionamientos, quiero configurar las zonas y espacios de cada estacionamiento para representar su distribución física en Quadrapp.

  Escenario: Escenario 1: Crear zona.
    Dado un estacionamiento registrado en la institución
    Cuando el administrador crea una nueva zona
    Entonces la zona queda asociada a ese estacionamiento.

  Escenario: Escenario 3: Espacio duplicado.
    Dado que un espacio con la misma etiqueta ya existe dentro de la zona
    Cuando el administrador intenta registrarlo nuevamente
    Entonces el sistema impide la duplicación.
