# Auth System

Java / Spring Boot 會員系統 API：Email 註冊（含開通信）、密碼 + Email OTP 兩階段登入、查詢自己的最後登入時間。

## 功能

- `POST /api/auth/register` — 註冊，寄送開通信
- `GET  /api/auth/activate?token=...` — 透過信件連結開通帳號
- `POST /api/auth/login` — 驗證密碼後寄送 6 碼 OTP
- `POST /api/auth/verify-otp` — 驗證 OTP，回傳 JWT
- `GET  /api/users/me/last-login` — （需 JWT）查詢上次登入時間
- Swagger UI：`/swagger-ui.html`
- 靜態前端：`/index.html`、`/login.html`、`/verify-otp.html`、`/dashboard.html`

## 技術棧

- Java 17、Spring Boot 3.3、Spring Security、Spring Data JPA
- jjwt（JWT 簽發/驗證）、BCrypt（密碼雜湊）
- H2（dev profile，記憶體）、PostgreSQL（prod profile）
- Mailtrap（Email；sandbox 模式攔截信件不真寄）
- springdoc-openapi（Swagger UI）

## 本機執行

需要：JDK 17+、Maven（或用 IDE 直接跑）。

```bash
cp .env.example .env       # 自行填入 Mailtrap / JWT secret
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run     # 或：mvn spring-boot:run
```

- 開 http://localhost:8080/index.html 體驗完整流程
- Swagger：http://localhost:8080/swagger-ui.html
- H2 console：未開啟（用 dev profile 時資料庫在記憶體裡）

> 若沒設 `MAILTRAP_API_TOKEN`，Email 內容會直接 log 到 console（含 activation link 與 OTP），方便本機測試。

## Mailtrap 申請

1. 到 https://mailtrap.io 註冊（免費方案 1000 封/月，不需驗證寄件網域）
2. 左側選 **Email Testing → Inboxes**，建立 inbox（或使用預設 Demo Inbox）
3. 點 inbox 右上 **Show Credentials → API**，取得：
   - **API Token** → `MAILTRAP_API_TOKEN`
   - **Inbox ID**（URL 末段的數字） → `MAILTRAP_INBOX_ID`
4. `MAILTRAP_API_BASE_URL` 預設指向 `https://sandbox.api.mailtrap.io`（攔截信件）。
   想要真實寄信到收件人 Email，需驗證網域並改成 `https://send.api.mailtrap.io`。

## 部署到 Render（建議）

1. 把專案 push 到 GitHub
2. Render Dashboard → **New → Web Service** → 連 GitHub repo
3. 設定：
   - Environment：Docker（或 Native Java；Spring Boot 自動偵測）
   - Build Command：`./mvnw clean package -DskipTests`
   - Start Command：`java -jar target/*.jar`
4. **New → PostgreSQL** 建立 DB，取得 internal connection string
5. 在 Web Service → Environment 設定環境變數：

   | Key | Value |
   |-----|-------|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `SPRING_DATASOURCE_URL` | Render Postgres internal URL（換成 `jdbc:postgresql://...`） |
   | `SPRING_DATASOURCE_USERNAME` | Render Postgres user |
   | `SPRING_DATASOURCE_PASSWORD` | Render Postgres password |
   | `JWT_SECRET` | `openssl rand -base64 64` 產出的字串 |
   | `MAILTRAP_API_TOKEN` | 從 Mailtrap 取得 |
   | `MAILTRAP_INBOX_ID` | 從 Mailtrap 取得 |
   | `APP_BASE_URL` | `https://<your-app>.onrender.com` |

6. 部署成功後：
   - 前端：`https://<your-app>.onrender.com/index.html`
   - Swagger：`https://<your-app>.onrender.com/swagger-ui.html`

### 冷啟動防睡

Render 免費 instance 閒置 15 分鐘會 sleep，第一個 request 要 20-30 秒喚醒。
用 https://cron-job.org 設每 14 分鐘 GET `https://<your-app>.onrender.com/actuator/health` 保持喚醒。

## API 範例

```bash
# 註冊
curl -X POST localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Pass1234"}'

# 開通（點信件中的連結，或直接呼叫）
curl "localhost:8080/api/auth/activate?token=<from-email>"

# 登入（會寄 OTP）
curl -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Pass1234"}'

# 驗 OTP，拿 JWT
curl -X POST localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","otp":"123456"}'

# 查詢上次登入時間
curl localhost:8080/api/users/me/last-login \
  -H "Authorization: Bearer <jwt>"
```

## 設計筆記

- **`previous_login_at`**：查詢 API 需要 JWT，而 JWT 是當下登入才拿到的，所以 `last_login_at` 永遠是「剛剛」。實際回傳給使用者的是「上次登入時間」（也就是「上一次」`last_login_at` 的值）。第一次登入時為 `null`。
- **OTP 安全**：5 分鐘過期、5 次嘗試失敗作廢，避免暴力破解。
- **JWT**：1 小時有效期、HS256，無 refresh token（題目沒要求）。
- **不查詢他人的最後登入時間**：API path 是 `/users/me/...`，從 JWT subject 取 userId，沒有 `/users/{id}/...`，自然就無法查別人。
