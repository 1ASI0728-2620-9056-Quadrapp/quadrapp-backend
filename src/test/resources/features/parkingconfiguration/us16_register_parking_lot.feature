# language: es
@US16 @EP04
Característica: Registrar estacionamiento universitario
  Como administrador de estacionamientos, quiero registrar los estacionamientos de la universidad para que puedan ser utilizados por Quadrapp.

  Escenario: Escenario 1: Registro exitoso.
    Dado que el administrador proporciona los datos obligatorios
    Cuando registra un estacionamiento
    Entonces el sistema crea el estacionamiento correctamente.

  Escenario: Escenario 3: Registro duplicado.
    Dado que la institución ya tiene un estacionamiento registrado con el mismo nombre
    Cuando el administrador intenta registrar otro con ese nombre
    Entonces el sistema impide la duplicación dentro de esa institución.
