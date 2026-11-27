# Nitrate Backend

Este é o back-end de um projeto de recomendação de filmes. A API foi feita com Spring Boot e usa PostgreSQL para guardar usuários, filmes e informações relacionadas, como gêneros, atores e diretores.

O projeto ainda está em desenvolvimento e faz parte de uma aplicação maior. O código é proprietário: veja [Licença](#licença).

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

Para usar outro banco, altere `spring.datasource.url` no `application.yaml`.

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
    "username": "lucas",
    "email": "lucas@example.com",
    "password": "minha-senha",
    "acceptedTerms": true
  }'
```

`acceptedTerms` precisa ser `true`: é a confirmação da idade mínima (13 anos) e do aceite dos Termos de Uso e da Política de Privacidade. A data do aceite fica guardada na conta.

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
    "login": "lucas@example.com",
    "password": "minha-senha",
    "rememberMe": true
  }'
```

A resposta possui o mesmo formato do cadastro. `login` aceita o e-mail ou o nome de usuário. Com `rememberMe: true` ("Lembrar de mim"), o token vale 30 dias (`JWT_REMEMBER_EXPIRATION_MS`), e não 24h; trocar a senha ou o e-mail devolve um token com a mesma duração do atual. O token identifica a conta pelo id; tokens do formato antigo (com o e-mail) são recusados.

Senhas erradas: 10 em 15 minutos bloqueiam o login da conta a partir daquele IP, e 50 de IPs diferentes bloqueiam a conta inteira pelo mesmo tempo (`429`).

### `POST /auth/logout`

Encerra o token enviado no header `Authorization`: ele deixa de valer na hora, e não só quando vencer. Responde sempre `204`, mesmo sem token ou com um já vencido.

### `POST /auth/forgot-password`

Pede a redefinição de senha. Se houver conta com o e-mail, envia pelo Resend um link para `FRONTEND_URL/redefinir-senha?token=...`, que vale por 30 minutos e só pode ser usado uma vez. A resposta é sempre `204 No Content`, exista a conta ou não, para não revelar quem está cadastrado.

```bash
curl -X POST http://localhost:8080/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{ "email": "lucas@example.com" }'
```

Sem `RESEND_API_KEY` (desenvolvimento), o e-mail não é enviado e o link aparece no log.

### `POST /auth/reset-password`

Troca a senha com o token do link. Responde `204`, ou `400` se o token for inválido ou tiver expirado. Os tokens JWT emitidos antes da troca deixam de valer, o que encerra as sessões abertas em outros aparelhos.

```bash
curl -X POST http://localhost:8080/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{ "token": "TOKEN_DO_LINK", "password": "nova-senha-123" }'
```

### `PUT /users/me/profile`

Edita o perfil: `name`, `username`, `bio`, `twitter` e `instagram`. O `username` segue as regras do cadastro (letras maiúsculas ou minúsculas, números e `_`; "Ana" e "ana" contam como o mesmo @) e responde `409` se já estiver em uso; se vier nulo, o @ atual é mantido. Trocar o @ não encerra a sessão, porque o token guarda o id da conta.

### `PUT /users/me/password`

Troca a senha sabendo a atual: `{ "currentPassword": "...", "newPassword": "..." }`. Responde `403` se a senha atual estiver errada e `429` depois de 10 tentativas em 15 minutos. Encerra as sessões nos outros aparelhos, avisa por e-mail e devolve um token novo (mesmo formato do login) para quem trocou continuar conectado.

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

Expande o grafo a partir dos filmes curtidos ou bem avaliados que ainda estão pendentes: importa os filmes mais conhecidos do diretor e do elenco principal. Processa por até 90 segundos e informa quantos faltam, então pode ser chamado de novo até `sementesRestantes` chegar a zero. Em produção, uma tarefa agendada chama esse endpoint uma vez por dia (veja [Tarefas agendadas](#tarefas-agendadas)).

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

Por padrão a API aceita requisições de `http://localhost:3000`. Para outras origens, configure `cors.allowed-origins` (em produção, `CORS_ALLOWED_ORIGINS`) com uma lista separada por vírgulas. Curingas são aceitos, por exemplo `https://nitrate.com.br,https://*.nitrate.com.br`.

## Deploy (VPS com Coolify)

A API está no ar em [api.nitrate.com.br](https://api.nitrate.com.br/health), numa VPS da Hostinger gerenciada pelo [Coolify](https://coolify.io). O front ([nitrate.com.br](https://nitrate.com.br)) roda na mesma VPS, também em Docker.

O Coolify constrói o `Dockerfile` da raiz. A imagem usa o perfil `prod` (`application-prod.yaml`), que lê toda a configuração de variáveis de ambiente. O `application.yaml` local nunca entra na imagem (`.dockerignore`).

1. No Coolify, crie um recurso a partir deste repositório com o build pack **Dockerfile** e a porta `8080`.
2. Aponte o domínio da API (ex.: `https://api.nitrate.com.br`) para o recurso; o Coolify emite o certificado HTTPS.
3. Em *Environment Variables*, configure:

| Variável               | Exemplo / observação                                                        |
| ---------------------- | --------------------------------------------------------------------------- |
| `DATABASE_URL`         | `jdbc:postgresql://HOST:5432/nitrate`                                       |
| `DB_USER`              | usuário do banco                                                            |
| `DB_PASSWORD`          | senha do banco                                                              |
| `TMDB_API_KEY`         | chave da API do TMDB                                                        |
| `JWT_SECRET`           | segredo do JWT, com pelo menos 32 caracteres                                |
| `CRON_SECRET`          | segredo do `/admin/**`, enviado pelas tarefas agendadas                     |
| `CORS_ALLOWED_ORIGINS` | domínio(s) do front, ex.: `https://nitrate.com.br`                          |
| `ADMIN_USER_IDS`       | ids das contas que abrem o painel de admin no site, separados por vírgula   |
| `JWT_EXPIRATION_MS`    | opcional, padrão `86400000` (24h)                                           |
| `JWT_REMEMBER_EXPIRATION_MS` | opcional, padrão `2592000000` (30 dias), com "Lembrar de mim" no login      |
| `RESEND_API_KEY`       | chave do [Resend](https://resend.com), para os e-mails de redefinir a senha |
| `EMAIL_FROM`           | remetente de um domínio verificado no Resend, ex.: `Nitrate <nao-responda@seudominio.com>` |
| `FRONTEND_URL`         | endereço do front, usado no link do e-mail, ex.: `https://nitrate.com.br`   |
| `PORT`                 | opcional, padrão `8080`                                                     |

4. No front, configure `NEXT_PUBLIC_API_URL` com a URL da API (é um argumento do build, então mudar o valor exige um build novo).

### Tarefas agendadas

Os jobs diários são endpoints do `/admin/**` e precisam ser chamados de fora, com o `CRON_SECRET` (em *Scheduled Tasks* no Coolify ou no `crontab` da VPS):

| Horário (UTC) | Endpoint                 |
| ------------- | ------------------------ |
| 06:00         | `/admin/expand-graph`    |
| 07:00         | `/admin/release-dates`   |
| 12:00         | `/admin/release-radar`   |

```bash
curl -fsS -H "Authorization: Bearer $CRON_SECRET" https://api.nitrate.com.br/admin/expand-graph
```

Como a API roda como um processo contínuo, o grafo fica em memória e é remontado a cada 10 minutos, e a expansão disparada ao curtir um filme roda em segundo plano até o fim. O job diário de `/admin/expand-graph` só termina o que tiver ficado pendente (por exemplo, depois de um redeploy).

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

## Licença

Copyright (c) 2026 Lucas Barros Simon. Todos os direitos reservados. O código está público apenas para consulta; copiar, modificar, redistribuir ou usar qualquer parte dele sem autorização por escrito é proibido. Veja o arquivo [LICENSE](LICENSE).
