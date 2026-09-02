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
curl -X POST "http://localhost:8080/admin/import-movies?paginas=2"
```

Resposta:

```text
34 filme(s) importado(s) da TMDB.
```

Filmes que já estão no banco são ignorados. É necessário configurar `TMDB_API_KEY`.

Apesar do nome `/admin`, esse endpoint está liberado na configuração atual e não exige um usuário ADMIN. Isso deve ser corrigido antes de usar a aplicação em produção.

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

Durante o desenvolvimento, a API permite requisições da origem `http://localhost:3000`. Para usar outra origem, altere a configuração de CORS em `SecurityConfig`.

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
