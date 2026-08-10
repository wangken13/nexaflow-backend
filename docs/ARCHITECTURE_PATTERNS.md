# NexaFlow Design Patterns

The project uses GoF patterns where they reduce coupling or make the failure path explicit. The goal is not to force all 23 patterns into ordinary CRUD code.

| Pattern | Implementation | Purpose |
| --- | --- | --- |
| Strategy | `SmsSender`, `AiProviderStrategy`, `FileObjectStorage`, `RateLimitCounterStore` | Select an external provider or fallback without changing the business service. |
| Factory Method | `InquiryDraftFactory` | Creates customer-facing analysis drafts from a single business input. |
| Facade | `AuthService` | Exposes one authentication workflow over captcha, password hashing, sessions, SMS, and OAuth. |
| Adapter | `SpugSmsSender`, `ConfiguredSmsSender`, `MinioFileObjectStorage` | Adapts third-party SDK/protocol details to domain interfaces. |
| Chain of Responsibility | Gateway filters, servlet filters | Runs identity verification, rate limiting, security headers, and audit recording in a controlled order. |
| Command | `InquiryCreatedEvent` | Represents an asynchronous request for AI analysis. |
| Observer | RabbitMQ listener and event routing | Decouples inquiry creation from AI processing. |
| State | `PENDING -> PUBLISHING -> PUBLISHED` in `integration_outbox` | Makes asynchronous delivery recoverable and observable. |
| Proxy | OpenFeign clients | Provides service-to-service calls through typed interfaces. |
| Builder | JWT and MinIO SDK builders | Builds security-sensitive request objects explicitly. |
| Singleton | Spring-managed stateless components | Shares safe, stateless service implementations. |

Patterns such as Memento, Interpreter, Flyweight, Visitor, and Prototype are intentionally not introduced: the current domain has no state snapshots, DSL, object graph traversal, or high-volume object sharing requirement. Adding them would make the code harder to understand without improving reliability.

## Logging Rules

- Log security decisions, stable internal IDs, tenant IDs, status transitions, and retry causes.
- Never log passwords, SMS codes, bearer tokens, refresh cookies, phone numbers, full email addresses, or raw file contents.
- Use structured `key=value` fields so logs remain searchable in a central log platform.
