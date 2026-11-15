# language: es
@US30 @EP01
Característica: Gestionar los dominios de correo habilitados
  Como administrador de estacionamientos, quiero gestionar los dominios de correo institucional habilitados para mi universidad para controlar quién puede registrarse en Quadrapp.

  Escenario: Escenario 1: Agregar un dominio.
    Dado que el administrador pertenece a una institución registrada
    Cuando agrega un dominio de correo institucional
    Entonces el sistema lo habilita para el registro de nuevos usuarios de esa institución.

  Escenario: Escenario 3: Dominio en uso por otra institución.
    Dado que el dominio ya está habilitado para otra institución
    Cuando el administrador intenta agregarlo
    Entonces el sistema rechaza la solicitud e informa el conflicto.
