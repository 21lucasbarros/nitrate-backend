# Nitrate Backend

Este é o back-end de um projeto de recomendação de filmes. A API foi feita com Spring Boot e usa PostgreSQL para guardar usuários, filmes e informações relacionadas, como gêneros, atores e diretores.

O projeto ainda está em desenvolvimento e faz parte de uma aplicação maior. A licença ainda não foi definida.

## Tecnologias

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security e JWT
- PostgreSQL
- API do TMDB

## Configuração

É necessário ter o JDK 21, PostgreSQL e, se for importar filmes, uma chave da API do TMDB.

O arquivo `src/main/resources/application.yaml` usa estas variáveis:

| Variável       | Uso                                    |
| -------------- | -------------------------------------- |
| `DB_USER`      | Usuário do PostgreSQL                  |
| `DB_PASSWORD`  | Senha do PostgreSQL                    |
| `TMDB_API_KEY` | Chave da API do TMDB                   |
| `JWT_SECRET`   | Chave usada para assinar os tokens JWT |

Exemplo:

```bash
export DB_USER="postgres"
export DB_PASSWORD="minha-senha"
export TMDB_API_KEY="minha-chave-tmdb"
export JWT_SECRET="uma-chave-secreta-com-pelo-menos-32-bytes"
```

O banco configurado atualmente é um PostgreSQL hospedado na Neon. Para usar outro banco, altere `spring.datasource.url` no `application.yaml`.

Durante o desenvolvimento, o Hibernate usa `ddl-auto: update` e atualiza as tabelas automaticamente. Não coloque senhas ou chaves no Git.

## Executando

Na raiz do projeto:

```bash
./mvnw spring-boot:run
```

Caso o Maven esteja instalado, também pode ser usado:

```bash
mvn spring-boot:run
```

A API fica disponível em `http://localhost:8080`.

Para testar se ela está funcionando:

```bash
curl http://localhost:8080/health
```

Resposta:

```text
Nitrate backend está no ar!
```

Para gerar o arquivo `.jar`:

```bash
mvn clean package
java -jar target/nitrate-0.0.1-SNAPSHOT.jar
```

## Autenticação

O cadastro e o login retornam um token JWT. Nos endpoints protegidos, o token deve ser enviado assim:

```http
Authorization: Bearer SEU_TOKEN
```

O token expira depois de aproximadamente 24 horas.

## Endpoints

### `POST /auth/register`

Cadastra um usuário. Não precisa de autenticação.

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Lucas Barros",
    "email": "lucas@example.com",
    "password": "minha-senha"
  }'
```

Resposta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "name": "Lucas Barros",
  "email": "lucas@example.com"
}
```

Se o e-mail já existir, a API retorna `409 Conflict`.

### `POST /auth/login`

Faz login e gera um novo token JWT. Também não precisa de autenticação.

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "lucas@example.com",
    "password": "minha-senha"
  }'
```

A resposta possui o mesmo formato do cadastro.

### `GET /movies`

Lista todos os filmes cadastrados. É um endpoint público.

```bash
curl http://localhost:8080/movies
```

Exemplo de resposta:

```json
[
  {
    "id": 1,
    "title": "Inception",
    "releaseYear": 2010,
    "rating": 8.4,
    "director": "Christopher Nolan",
    "franchise": null,
    "genres": ["Action", "Science Fiction"]
  }
]
```

### `GET /movies/{id}`

Busca um filme pelo ID. Também é público.

```bash
curl http://localhost:8080/movies/1
```

Se o filme não existir, retorna `404 Not Found`.

### `POST /recommendations`

Gera recomendações e precisa de um token JWT.

```bash
curl -X POST http://localhost:8080/recommendations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN" \
  -d '{
    "likedMovieIds": [1, 7],
    "favoriteDirectors": ["Christopher Nolan"],
    "favoriteActors": ["Leonardo DiCaprio"]
  }'
```

Campos do corpo:

| Campo               | Tipo             | Descrição               |
| ------------------- | ---------------- | ----------------------- |
| `likedMovieIds`     | Lista de números | IDs dos filmes curtidos |
| `favoriteDirectors` | Lista de textos  | Diretores favoritos     |
| `favoriteActors`    | Lista de textos  | Atores favoritos        |

As listas podem ficar vazias. A API retorna no máximo cinco recomendações e não recomenda novamente os filmes curtidos.

Exemplo de resposta:

```json
[
  {
    "movieId": 12,
    "title": "The Prestige",
    "score": 4.65,
    "reasons": [
      "Mesmo diretor: Christopher Nolan",
      "Mesmo gênero: Science Fiction"
    ]
  }
]
```

O `score` é uma pontuação interna do algoritmo, não a nota do TMDB.

### `POST /admin/import-movies?paginas=2`

Importa filmes populares do TMDB. O parâmetro `paginas` é opcional e o padrão é `2`.

```bash
curl -X POST -H "Authorization: Bearer $CRON_SECRET" "http://localhost:8080/admin/import-movies?paginas=2"
```

Resposta:

```text
34 filme(s) importado(s) da TMDB.
```

Filmes que já estão no banco são ignorados. É necessário configurar `TMDB_API_KEY`.

Todos os endpoints `/admin/**` exigem o header `Authorization: Bearer <segredo>`, com o valor de `admin.secret` (em produção, a variável `CRON_SECRET`). Sem segredo configurado, o `/admin` fica bloqueado.

### `GET|POST /admin/expand-graph`

Expande o grafo a partir dos filmes curtidos ou bem avaliados que ainda estão pendentes: importa os filmes mais conhecidos do diretor e do elenco principal. Processa por até 90 segundos e informa quantos faltam, então pode ser chamado de novo até `sementesRestantes` chegar a zero. Na Vercel, um Cron Job chama esse endpoint uma vez por dia (veja `vercel.json`).

```json
{ "sementesProcessadas": 3, "filmesNovos": 41, "sementesRestantes": 0 }
```

## Erros

Alguns erros retornam este formato:

```json
{
  "timestamp": "2026-09-02T12:00:00Z",
  "status": 404,
  "error": "Recurso não encontrado",
  "message": "Filme não encontrado. Id: 999",
  "path": "/movies/999"
}
```

Principais códigos:

| Código | Significado                         |
| ------ | ----------------------------------- |
| `401`  | Token ausente, inválido ou expirado |
| `404`  | Filme não encontrado                |
| `409`  | E-mail já cadastrado                |

## Fluxo básico

1. Configure o banco e as variáveis de ambiente.
2. Inicie a aplicação.
3. Importe alguns filmes pelo endpoint do TMDB.
4. Cadastre um usuário ou faça login.
5. Consulte `/movies` para ver os IDs disponíveis.
6. Use o token para chamar `/recommendations`.

## CORS

Por padrão a API aceita requisições de `http://localhost:3000`. Para outras origens, configure `cors.allowed-origins` (em produção, `CORS_ALLOWED_ORIGINS`) com uma lista separada por vírgulas. Curingas são aceitos, por exemplo `https://nitrate.vercel.app,https://nitrate-*.vercel.app`.

## Deploy na Vercel (Docker)

A Vercel constrói o `Dockerfile` da raiz com o [Container Runtime](https://vercel.com/docs/functions/runtimes/container) (beta). A imagem usa o perfil `prod` (`application-prod.yaml`), que lê toda a configuração de variáveis de ambiente. O `application.yaml` local nunca entra na imagem (`.dockerignore` e `.vercelignore`).

1. Importe este repositório como um projeto novo na Vercel. Como não há outro framework, ela escolhe o preset **Container**.
2. Em *Settings → Environment Variables*, configure:

| Variável               | Exemplo / observação                                                        |
| ---------------------- | --------------------------------------------------------------------------- |
| `PORT`                 | `8080` (obrigatória: sem ela a Vercel envia o tráfego para a porta 80)      |
| `DATABASE_URL`         | `jdbc:postgresql://HOST.neon.tech/neondb?sslmode=require`                   |
| `DB_USER`              | usuário do banco                                                            |
| `DB_PASSWORD`          | senha do banco                                                              |
| `TMDB_API_KEY`         | chave da API do TMDB                                                        |
| `JWT_SECRET`           | segredo do JWT, com pelo menos 32 caracteres                                |
| `CRON_SECRET`          | segredo do `/admin/**`; a Vercel o envia nos Cron Jobs                      |
| `CORS_ALLOWED_ORIGINS` | domínio(s) do front, ex.: `https://nitrate.vercel.app`                      |
| `JWT_EXPIRATION_MS`    | opcional, padrão `86400000` (24h)                                           |

3. No projeto do front, configure `NEXT_PUBLIC_API_URL` com a URL do backend.

Como a API roda como uma função, alguns pontos mudam em relação a um servidor tradicional:

- **Desliga quando ociosa:** sem tráfego por 5 minutos (produção), a instância é desligada. A requisição seguinte espera a JVM e o Spring iniciarem.
- **Várias instâncias:** cada uma tem o próprio grafo em memória, que é remontado a cada 10 minutos.
- **Expansão do grafo:** a expansão disparada ao curtir um filme roda em segundo plano e pode ser interrompida quando a instância desliga. O cron diário em `/admin/expand-graph` termina o que ficou pendente.

Para testar a imagem localmente:

```bash
docker build -t nitrate-backend .
docker run -p 8080:8080 -e DATABASE_URL=... -e DB_USER=... -e DB_PASSWORD=... \
  -e TMDB_API_KEY=... -e JWT_SECRET=... -e CRON_SECRET=... \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000 nitrate-backend
```

## Testes

Para executar os testes:

```bash
mvn test
```

Os testes ficam em `src/test/java`.

## Estrutura principal

```text
src/main/java/com/lucasbarros/nitrate/
├── dto/            Objetos das requisições e respostas
├── entities/       Entidades do banco
├── graph/          Grafo usado nas recomendações
├── recommender/    Regras do recomendador
├── repositories/   Acesso ao banco
├── resources/      Controllers da API
├── security/       JWT e Spring Security
├── services/       Regras de negócio
└── tmdb/           Integração com o TMDB
```
