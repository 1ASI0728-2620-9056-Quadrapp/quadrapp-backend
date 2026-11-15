# language: es
@US19 @EP04
Característica: Configurar accesos vehiculares
  Como administrador de estacionamientos, quiero configurar los accesos de entrada y salida para registrar los movimientos de vehículos dentro del estacionamiento.

  Escenario: Escenario 1: Registrar acceso.
    Dado que el administrador configura un nuevo acceso
    Cuando proporciona sus datos
    Entonces el acceso queda asociado al estacionamiento correspondiente.
