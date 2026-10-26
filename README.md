# 🛡️ LazyHawk - Security Service

Este repositorio contiene el microservicio `security-service`, que forma parte de la arquitectura distribuida del sistema **LazyHawk**.

Este proyecto fue desarrollado como parte del **Sprint 2: Desacoplamiento de Microservicios, Seguridad e Infraestructura Distribuida**, para la cátedra de **Desarrollo y Arquitecturas Avanzadas de Software**, correspondiente a la carrera de Ingeniería en Informática de la Universidad Nacional de Jujuy (UNJu).

## 🎯 Objetivo y responsabilidad

El microservicio `security-service` es el **único responsable de la autenticación y gestión de usuarios** de la plataforma.

Sus principales responsabilidades son:

- Registrar nuevos usuarios.
- Proteger las credenciales mediante algoritmos de hash seguros.
- Autenticar usuarios y verificar sus credenciales.
- Emitir tokens JWT (*JSON Web Tokens*) firmados.
- Gestionar los roles y permisos de los usuarios.

Para garantizar un alto rendimiento y evitar cuellos de botella, este microservicio emite tokens cuya firma puede ser verificada por los demás microservicios, como `shipment-service` y `courier-service`.

De esta manera, los otros servicios pueden validar los tokens de forma local y autónoma, sin necesidad de realizar peticiones HTTP al `security-service` para verificar cada solicitud.

## 🚀 Características principales

- **Gestión estricta de roles:** soporte para los perfiles `ADMIN`, `CLIENTE` y `REPARTIDOR`.
- **Seguridad stateless:** implementación de Spring Security sin manejo de sesiones en memoria, basada en tokens JWT.
- **Filtros personalizados:** interceptación de peticiones mediante `JwtAuthenticationFilter` para validar firmas criptográficas y extraer el contexto de seguridad.
- **Hash de contraseñas:** protección de las contraseñas mediante `BCryptPasswordEncoder`.
- **Auditoría transaccional:** utilización de la superclase `EntidadAuditable` junto con JPA Auditing para registrar automáticamente las fechas de creación y modificación, además de identificadores UUID.
- **Manejo global de excepciones:** implementación de `@RestControllerAdvice` para capturar errores de negocio y seguridad, como `BadCredentialsException`, y devolver respuestas JSON estandarizadas.
- **Configuración centralizada:** integración con Spring Cloud Config Server para obtener propiedades de configuración desde un repositorio centralizado.
- **Persistencia independiente:** utilización de una base de datos propia, denominada `security_db`.
- **Contenerización:** preparación del servicio para su ejecución mediante Docker, utilizando una construcción multietapa (*multistage build*).

## 🛠️ Stack tecnológico

| Tecnología | Descripción |
|---|---|
| Java 25 | Lenguaje de programación |
| Spring Boot 4.2.0-SNAPSHOT | Framework principal |
| Spring Security | Autenticación y seguridad |
| JJWT | Generación y validación de tokens JWT |
| Spring Data JPA | Acceso y persistencia de datos |
| Hibernate | ORM para el mapeo objeto-relacional |
| MySQL | Sistema de gestión de bases de datos |
| Lombok | Reducción de código repetitivo |
| Jakarta Validation | Validación de datos de entrada |
| Spring Cloud Config | Configuración centralizada |
| Docker | Contenerización del servicio |
| Maven | Gestión de dependencias y construcción del proyecto |

## ⚙️ Configuración y ejecución local

### 1. Variables de entorno

Para ejecutar el microservicio localmente, es necesario configurar las siguientes variables de entorno en el IDE, por ejemplo, IntelliJ IDEA, o en el contenedor Docker.

```bash
DB_HOST=localhost
DB_PORT=3307
SECURITY_DB_PASSWORD=tu_password
CONFIG_SERVER_URL=http://localhost:8085
```

**Descripción de las variables:**

| Variable | Descripción |
|---|---|
| `DB_HOST` | Host donde se encuentra la base de datos MySQL |
| `DB_PORT` | Puerto de conexión a MySQL |
| `SECURITY_DB_PASSWORD` | Contraseña del usuario de la base de datos |
| `CONFIG_SERVER_URL` | URL del servidor de configuración centralizada |

> **Nota:** El puerto `3307` es un ejemplo. Debe coincidir con el puerto en el que esté expuesto el contenedor MySQL.

### 2. Dependencia del Config Server

El microservicio está configurado para consumir sus propiedades desde un repositorio Git centralizado mediante Spring Cloud Config Server.

Esta integración se configura en el archivo `application.yml`:

```yaml
spring:
  config:
    import: "optional:configserver:${CONFIG_SERVER_URL:http://localhost:8085}"
```

El modificador `optional:` permite que la aplicación intente arrancar sin el Config Server, siempre que disponga de las propiedades necesarias mediante otras fuentes de configuración.

**Importante:** si el servidor de configuración está apagado, la aplicación solo podrá arrancar si cuenta con los valores necesarios para iniciar correctamente.

### 3. Levantar la base de datos

El proyecto requiere una base de datos independiente denominada `security_db`.

Si el contenedor MySQL ya existe y está detenido, se puede iniciar mediante el siguiente comando:

```bash
docker start mysql-8.4.0
```

Para comprobar que el contenedor está en ejecución:

```bash
docker ps
```

**Importante:** antes de iniciar el microservicio, verificá que:

- El contenedor MySQL esté funcionando.
- La base de datos `security_db` exista.
- Las credenciales de conexión sean correctas.
- El puerto configurado coincida con el puerto publicado por Docker.
- El Config Server esté disponible o que la aplicación tenga las propiedades necesarias para arrancar sin él.

### 4. Ejecutar el microservicio

Desde la raíz del proyecto, ejecutá:

```bash
./mvnw spring-boot:run
```

Si el proyecto no incluye Maven Wrapper, podés utilizar Maven instalado en el sistema:

```bash
mvn spring-boot:run
```

También podés ejecutar la clase principal de Spring Boot directamente desde IntelliJ IDEA.

## 📡 Endpoints REST (API)

El microservicio expone los siguientes endpoints para la autenticación y gestión de la identidad del usuario.

| Método HTTP | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/registro` | Público | Registra un nuevo usuario y protege su contraseña mediante hash. Retorna `201 Created`. |
| `POST` | `/api/auth/login` | Público | Autentica al usuario y devuelve un token JWT firmado en formato JSON. |
| `GET` | `/api/auth/me` | Privado | Devuelve el perfil del usuario autenticado. Requiere un token JWT válido. |

### 🔓 Registro de usuarios

**Endpoint:**

```http
POST /api/auth/registro
```

Permite registrar un nuevo usuario en el sistema. La contraseña se almacena mediante un algoritmo de hash seguro, evitando guardar las credenciales en texto plano.

La operación retorna `201 Created` cuando el registro se completa correctamente.

### 🔓 Inicio de sesión

**Endpoint:**

```http
POST /api/auth/login
```

Permite autenticar a un usuario mediante sus credenciales.

Si las credenciales son válidas, el servicio genera un token JWT firmado y lo devuelve en formato JSON para que el cliente pueda utilizarlo en las siguientes solicitudes.

### 🔐 Obtener el perfil del usuario autenticado

**Endpoint:**

```http
GET /api/auth/me
```

Este endpoint está protegido y requiere un token JWT válido.

La solicitud debe incluir la siguiente cabecera HTTP:

```http
Authorization: Bearer <token>
```

Si el token es válido, el servicio devuelve la información del perfil correspondiente al usuario autenticado.

## 🏗️ Arquitectura y seguridad

El `security-service` forma parte de una arquitectura distribuida basada en microservicios.

Su responsabilidad se limita a la autenticación y gestión de usuarios, evitando concentrar en un único servicio la validación de todas las solicitudes del sistema.

### Flujo de autenticación

1. El usuario envía sus credenciales al endpoint de inicio de sesión.
2. El `security-service` verifica las credenciales utilizando los mecanismos de seguridad implementados.
3. Si la autenticación es exitosa, genera un token JWT firmado.
4. El cliente utiliza el token para acceder a los recursos protegidos.
5. Los microservicios correspondientes verifican el token y sus permisos antes de procesar las solicitudes.

Este enfoque permite reducir las comunicaciones entre microservicios y favorece una arquitectura desacoplada.

### Gestión de contraseñas

Las contraseñas se protegen utilizando `BCryptPasswordEncoder`, que aplica un algoritmo de hash diseñado para el almacenamiento seguro de credenciales.

Las contraseñas no deben almacenarse en texto plano.

### Autenticación stateless

La aplicación utiliza un modelo de autenticación sin estado (*stateless*).

Esto significa que la autenticación de cada solicitud se basa en la información y las credenciales presentadas, sin depender de una sesión de usuario almacenada en memoria en el servidor.

### Gestión de roles

El sistema contempla tres perfiles principales:

- `ADMIN`: administrador de la plataforma.
- `CLIENTE`: usuario que utiliza la plataforma para gestionar sus envíos.
- `REPARTIDOR`: usuario responsable de realizar las entregas.

Los permisos efectivos de cada perfil dependen de las reglas de autorización implementadas en la aplicación.

## 🐳 Docker

El proyecto contempla la utilización de Docker para facilitar la construcción y ejecución del microservicio en un entorno contenerizado.

La construcción multietapa (*multistage build*) permite separar las etapas de compilación y ejecución de la aplicación.

Las variables de entorno deben configurarse de acuerdo con el entorno en el que se despliegue el servicio.

## 📁 Estructura general del proyecto

La organización exacta puede variar según la implementación, pero un proyecto Spring Boot de estas características suele tener una estructura similar a la siguiente:

```text
security-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── .../
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── exception/
│   │   │       ├── filter/
│   │   │       ├── repository/
│   │   │       ├── security/
│   │   │       └── service/
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── Dockerfile
├── pom.xml
└── README.md
```

> **Nota:** esta estructura es ilustrativa. Los paquetes y archivos deben ajustarse a la organización real del repositorio.

## 👥 Equipo de desarrollo

**Proyecto:** LayHawk  
**Microservicio:** `security-service`  
**Sprint:** Sprint 2 — Desacoplamiento de Microservicios, Seguridad e Infraestructura Distribuida  
**Cátedra:** Desarrollo y Arquitecturas Avanzadas de Software  
**Carrera:** Ingeniería en Informática  
**Institución:** Universidad Nacional de Jujuy (UNJu)  
**Año:** 2026

**Desarrollado por:** LazyHawk Team.










