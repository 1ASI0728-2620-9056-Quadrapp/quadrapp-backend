# language: es
@US31 @EP01
Característica: Invitar administradores y operadores
  Como administrador de estacionamientos, quiero invitar a otros administradores y a los operadores de mi institución para que accedan a la consola con el rol que les corresponde.

  Escenario: Escenario 1: Invitación enviada.
    Dado que el administrador indica el correo y el rol de la persona invitada
    Cuando envía la invitación
    Entonces el sistema registra la invitación con su vigencia y envía el enlace de acceso a ese correo.

  Escenario: Escenario 3: Invitación vencida.
    Dado que la invitación superó su periodo de vigencia
    Cuando la persona invitada intenta utilizarla
    Entonces el sistema rechaza el acceso e informa que debe solicitar una nueva invitación.
