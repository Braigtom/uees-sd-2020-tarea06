# uees-sd-2020-tarea06


Braigtom Toral

Sergio Chavez

El objetivo de esta tarea es simular la comunicación entre un agente colector de mediciones de sensores y un servidor que recibirá las mediciones y las almacenará en una base de datos (o en archivos JSON). La comunicación entre los dos módulos se debe realizar mediante protocolo UDP, se deberá implementar las medidas necesarias para manejar efectivamente cualquier tipo de error de red (revisar los escenarios estudiados en la sesión de clases del 5 de Noviembre). Además, las aplicaciones deberán incluir código que permita inducir (simular) errores de la red, tanto en el emisor como en el receptor (revisar la parte final de la sesión de clases del 5 de Noviembre, donde se da un ejemplo de cómo se puede inducir esos errores).


## descarga

Desde la terminal linux del nodo 1 se procede a clonar el repositorio de la tareao con el comando 
 
```bash
git clone https://github.com/Braigtom/uees-sd-2020-tarea06.git
```

lo mismo desde la terminal del nodo 2.


## compilar proyecto

una vez descargado el proyecto en ambas maquinas virtuales se procederá a compilar en el nodo 1 con el siguiente comando 
   

```bash
 make servidor
java -jar servidor.jar
``` 

se procede a ejecutar lo mismo en el nodo 2 de la siguiente forma: 

```bash
 make controlador
java -jar controlador.jar
``` 
donde dice <ip> se tiene que proceder a a poner la direccion ip del nodo 1 (servidor) para poder hacer la conexion. y listo ambas maquinas ya estarán conectadas entre si esperando los ficheros json para su lectura.
                   


## Integrantes
Braigtom Toral

Sergio Chavez
