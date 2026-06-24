# CLAUDE.md — Contesto di Progetto

> Questo file viene letto automaticamente da Claude Code all'avvio.
> Aggiornalo man mano che il progetto evolve.

---

## Identità del Progetto

- **GroupId:** `com.sb`
- **ArtifactId:** `sfrigola-core`
- **Versione:** `0.0.1-SNAPSHOT`
- **Package base:** `com.sb.sfrigola_core`

---

## Stack Tecnologico

- **Framework:** Spring Boot 4.1.0
- **Linguaggio:** Java 25
- **Build tool:** Maven
- **Database:** PostgreSQL (driver `org.postgresql`)
- **ORM:** Spring Data JPA (Hibernate)
- **Sicurezza:** Spring Security
- **Validazione:** Spring Validation (Jakarta)
- **Web:** Spring MVC (`spring-boot-starter-webmvc`)
- **Utilità:** Lombok
- **Docker:** Spring Boot Docker Compose (avvio automatico dei container in dev)
- **Dev tools:** Spring Boot DevTools (hot reload)

### Dipendenze di Test

- `spring-boot-starter-data-jpa-test`
- `spring-boot-starter-security-test`
- `spring-boot-starter-validation-test`
- `spring-boot-starter-webmvc-test`

---

## Best Practice — Spring Boot

### Struttura del Progetto

Il progetto segue una **struttura a feature** (package-by-feature), non a layer.
Ogni feature è un package autonomo che contiene tutto ciò che la riguarda.

```
src/
└── main/
    └── java/com/sb/sfrigola_core/
        ├── auth/            # Registrazione, login, JWT, Spring Security, ruoli
        ├── language/        # CRUD lingue supportate, is_default, is_active
        ├── tag/             # Vocabulary controllato, traduzioni, flusso approvazione
        ├── category/        # Gerarchia categorie (self-referential), traduzioni
        ├── ingredient/      # Catalogo ingredienti, traduzioni, tag associati
        ├── recipe/          # Core feature: creazione, pubblicazione, ingredienti, tag
        ├── favorite/        # Utente salva/rimuove ricette, lista preferiti
        ├── rating/          # Voto 1-5 con commento opzionale, un voto per ricetta
        ├── stats/           # Aggregati pre-calcolati (NO controller — service interno)
        └── shared/          # Codice trasversale (eccezioni globali, config Spring)
```

**Regole:**
- Ogni package di feature è **coeso e autonomo**: nessun layer orizzontale globale (no `/controller`, no `/service`, ecc.).
- Le classi interne a una feature possono avere visibilità **package-private** se non servono all'esterno.
- Solo il codice realmente condiviso tra più feature va in `shared/`.

---

## Feature del Progetto

### 1. `auth`
- **Tabelle:** `users`, `roles`
- Registrazione, login, autenticazione JWT
- Ruoli: `ROLE_USER`, `ROLE_ADMIN`, `ROLE_CHEF`
- Integrazione con Spring Security

### 2. `language`
- **Tabelle:** `languages`
- CRUD lingue supportate dall'applicazione
- Gestione `is_default` e `is_active`

### 3. `tag`
- **Tabelle:** `tags`, `tag_translations`
- Vocabulary controllato con traduzioni per lingua
- Flusso di approvazione: `pending → approved / rejected`
- Operazioni di approvazione/rifiuto riservate ad `ROLE_ADMIN`

### 4. `category`
- **Tabelle:** `categories`, `category_translations`
- Gerarchia self-referential tramite `parent_id`
- Traduzioni per lingua, navigazione ad albero

### 5. `ingredient`
- **Tabelle:** `ingredients`, `ingredient_translations`, `ingredient_tags`
- Catalogo globale degli ingredienti
- Traduzioni per lingua
- Tag flavor / texture / season associati

### 6. `recipe` _(core feature)_
- **Tabelle:** `recipes`, `recipe_translations`, `recipe_ingredients`, `recipe_tags`
- Creazione e pubblicazione ricette
- Ingredienti con quantità, tag associati
- Traduzioni per lingua

### 7. `favorite`
- **Tabella:** `favorites`
- L'utente salva o rimuove ricette dai preferiti
- Lista preferiti personale per utente
- Al salvataggio/rimozione aggiorna `stats`

### 8. `rating`
- **Tabella:** `ratings`
- Voto da 1 a 5 con commento opzionale
- Un solo voto per utente per ricetta
- Al salvataggio aggiorna `stats`

### 9. `stats` _(service interno)_
- **Tabella:** `recipe_stats`
- Aggregati pre-calcolati (media voti, conteggio preferiti, popolarità)
- **Nessun controller REST** — viene chiamato internamente da `rating` e `favorite`
- Usato da `recipe` per ordinamento per popolarità

### Dependency Injection

- Preferire **constructor injection** rispetto a `@Autowired` su field.
- Usare `final` sui campi iniettati.
- Con Lombok, usare `@RequiredArgsConstructor` per semplicità.

```java
// ✅ Corretto
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
}

// ❌ Evitare
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}
```

### REST Controller

- Restituire sempre **DTO**, mai entità JPA direttamente.
- Usare `ResponseEntity<T>` per controllo esplicito degli status HTTP.
- Annotare con `@Validated` per la validazione degli input.
- Mantenere i controller **sottili**: nessuna logica di business.

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody @Valid CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.create(request));
    }
}
```

### Service Layer

- Tutta la **business logic** vive nel service.
- Usare `@Transactional` a livello di metodo (non di classe).
- Separare chiaramente lettura (`@Transactional(readOnly = true)`) da scrittura.

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional
    public UserDto create(CreateUserRequest request) {
        User user = userMapper.toEntity(request);
        return userMapper.toDto(userRepository.save(user));
    }
}
```

### Repository

- Estendere `JpaRepository<Entity, ID>` o `CrudRepository`.
- Usare **query derivate** per query semplici.
- Usare `@Query` con JPQL o native query per query complesse.
- Evitare query N+1: usare `JOIN FETCH` o `@EntityGraph`.

```java
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") Long id);
}
```

### Interfacce Service

Ogni interfaccia service deve essere completamente documentata con Javadoc seguendo queste regole:

- **Javadoc di classe**: descrivere il contratto generale dell'interfaccia, specificare se i metodi leggono il security context internamente o ricevono dati esplicitamente, e dichiarare il contratto "succeed or throw" se applicabile.
- **Javadoc di metodo**: ogni metodo deve avere descrizione, `@param` per ogni parametro, `@return` che descrive il valore restituito, e `@throws` per ogni eccezione che può essere lanciata.
- **Contratto "succeed or throw"**: i metodi write che non hanno un caso legittimo di fallimento silenzioso devono dichiararlo esplicitamente nel Javadoc di classe. Usare `@return {@code true} on success` — mai scrivere `{@code false} otherwise` se il metodo lancia eccezione invece di tornare false.
- **`@throws` inline**: usare sempre il fully-qualified name direttamente nel tag `@throws` — nessun import aggiuntivo in cima al file solo per la documentazione.
- **Distinzione di contratto**: separare chiaramente le interfacce controller-facing (leggono security context) da quelle internal bridge (ricevono dati grezzi). Documentare questa distinzione nel Javadoc di classe.

```java
/**
 * Controller-facing contract for X operations.
 * All methods read the authenticated user from the security context internally.
 * Write methods follow the "succeed or throw" contract: return {@code true} on success,
 * throw a specific exception if any step fails.
 */
public interface IXService {

    /**
     * Does something for the authenticated user.
     *
     * @param param description
     * @return {@code true} if the operation succeeded
     * @throws SpecificException if the specific condition occurs
     */
    boolean doSomething(String param);
}
```

### Gestione Eccezioni

- Usare un **handler globale** con `@RestControllerAdvice`.
- Definire eccezioni custom che estendono `RuntimeException`.
- Restituire sempre un body strutturato negli errori.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Validation failed", errors));
    }
}
```

### Configurazione

- Non scrivere mai **secrets o password** in `application.properties`.
- Usare **profili** per separare gli ambienti: `dev`, `staging`, `prod`.
- Esternalizzare la configurazione sensibile con variabili d'ambiente o secrets manager.

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILE:dev}

# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sfrigola
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

### Validazione Input

- Usare le annotazioni di **Jakarta Validation** sui DTO.
- Attivare la validazione con `@Valid` o `@Validated` nei controller.

```java
public record CreateUserRequest(
    @NotBlank(message = "Il nome è obbligatorio")
    String name,

    @Email(message = "Email non valida")
    @NotBlank
    String email,

    @Size(min = 8, message = "La password deve avere almeno 8 caratteri")
    String password
) {}
```

### Sicurezza

- **Spring Security è già incluso** nel progetto — ogni endpoint è protetto per default.
- Configurare una `SecurityFilterChain` esplicita in `config/SecurityConfig.java`.
- Usare JWT o OAuth2 per autenticazione stateless nelle API REST.
- Non esporre stack trace nelle risposte di errore in produzione.
- Abilitare HTTPS in produzione.
- Configurare CORS esplicitamente, non usare wildcard `*` in produzione.

### Docker Compose (Sviluppo Locale)

- Il progetto include `spring-boot-docker-compose`: Spring Boot avvia automaticamente i container definiti in `compose.yaml` al lancio dell'app in dev.
- Definire PostgreSQL in `compose.yaml`:

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: sfrigola
      POSTGRES_USER: ${DB_USER:-sfrigola}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-secret}
    ports:
      - "5432:5432"
```

### Logging

- Usare **SLF4J** (non `System.out.println`).
- Con Lombok: `@Slf4j` sulla classe.
- Livelli: `DEBUG` in sviluppo, `INFO/WARN` in produzione.
- Non loggare mai dati sensibili (password, token, dati personali).

```java
@Slf4j
@Service
public class UserService {
    public UserDto findById(Long id) {
        log.debug("Ricerca utente con id: {}", id);
        // ...
    }
}
```

### Testing

- **Unit test** su service e componenti con JUnit 5 + Mockito.
- **Integration test** con `@SpringBootTest` + Testcontainers per il DB.
- **Controller test** con `@WebMvcTest` + MockMvc.
- Mirare a coverage significativa sulla business logic, non al numero.

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.findById(99L));
    }
}
```

### Performance

- Usare **paginazione** (`Pageable`) per liste potenzialmente grandi.
- Abilitare il **caching** con `@Cacheable` dove appropriato.
- Monitorare le query SQL lente in sviluppo con `spring.jpa.show-sql=true`.
- In produzione usare strumenti come **Actuator + Micrometer**.

---

## Convenzioni di Codice

- Nomi classi: `PascalCase`
- Nomi metodi e variabili: `camelCase`
- Costanti: `UPPER_SNAKE_CASE`
- Package: `lowercase`
- Commit: conventional commits in inglese (`feat:`, `fix:`, `refactor:`, ecc.)
- Ogni PR deve avere almeno un test associato

---

## Comandi Utili

```bash
# Avvio in locale
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Build
./mvnw clean package -DskipTests

# Test
./mvnw test

# Test con profilo specifico
./mvnw test -Dspring.profiles.active=test
```

---

## Note per Claude

- Seguire sempre la **struttura a feature**: ogni nuova funzionalità va nel proprio package, non in layer globali.
- Non creare cartelle `/controller`, `/service`, `/repository` a livello root — tutto dentro la feature.
- Non inserire logica di business nei controller.
- Restituire sempre DTO nelle API, mai entità JPA.
- Preferire record Java per DTO e request immutabili.
- `stats` non ha controller: è un service interno chiamato da `rating` e `favorite`, mai esposto via REST.
- Il flusso di approvazione dei `tag` (`pending → approved/rejected`) è riservato a `ROLE_ADMIN`.
- Le traduzioni (tag, category, ingredient, recipe) sono sempre associate a una lingua definita in `language`.
- Segnalare se una richiesta viola queste convenzioni prima di procedere.