# Deploy Backend lên Railway

Frontend đã ở Vercel → Backend + MySQL deploy trên Railway.

## 1. Push code lên GitHub

```powershell
cd synctube
git init
git add .
git commit -m "Initial commit: SyncTube backend"
git branch -M main
git remote add origin https://github.com/buiquangkiet/sync-tube-be.git
git push -u origin main
```

(Tạo repo `sync-tube-be` trên GitHub trước nếu chưa có.)

## 2. Tạo project Railway

1. [railway.app](https://railway.app) → đăng nhập GitHub
2. **New Project** → **Deploy from GitHub repo** → chọn `sync-tube-be`
3. Railway tự detect **Dockerfile** và build

## 3. Thêm MySQL

1. Trong project → **+ New** → **Database** → **MySQL**
2. Đợi MySQL chạy xong
3. Click service **Backend** → **Variables** → **Add Reference** (hoặc **Raw Editor**)

Thêm biến tham chiếu MySQL (tên service MySQL có thể là `MySQL`):

| Variable | Value (Railway reference) |
|----------|---------------------------|
| `MYSQLHOST` | `${{MySQL.MYSQLHOST}}` |
| `MYSQLPORT` | `${{MySQL.MYSQLPORT}}` |
| `MYSQLUSER` | `${{MySQL.MYSQLUSER}}` |
| `MYSQLPASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `MYSQLDATABASE` | `${{MySQL.MYSQLDATABASE}}` |

> Hoặc set thủ công `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` từ tab **Connect** của MySQL (đổi `mysql://` → `jdbc:mysql://` và thêm `?useSSL=true&allowPublicKeyRetrieval=true`).

## 4. Biến môi trường Backend

Trong service Backend → **Variables**:

| Variable | Giá trị |
|----------|---------|
| `SPRING_PROFILES_ACTIVE` | `oauth,prod` |
| `FRONTEND_URL` | `https://your-app.vercel.app` |
| `JWT_SECRET` | chuỗi ngẫu nhiên ≥ 32 ký tự |
| `GOOGLE_CLIENT_ID` | từ Google Console |
| `GOOGLE_CLIENT_SECRET` | |
| `FACEBOOK_CLIENT_ID` | (tuỳ chọn) |
| `FACEBOOK_CLIENT_SECRET` | |

`PORT` — Railway tự inject, không cần set.

## 5. Public domain

1. Service Backend → **Settings** → **Networking** → **Generate Domain**
2. Copy URL, ví dụ: `https://synctube-be-production.up.railway.app`

## 6. Cập nhật Vercel (Frontend)

Vercel → Project → **Settings** → **Environment Variables**:

| Variable | Value |
|----------|--------|
| `VITE_API_BASE_URL` | `https://synctube-be-production.up.railway.app/api` |
| `VITE_WS_URL` | `wss://synctube-be-production.up.railway.app/ws` |
| `VITE_OAUTH_BASE_URL` | `https://synctube-be-production.up.railway.app` |
| `VITE_ENABLE_WEBSOCKET` | `true` |

**Deployments** → **Redeploy** (bắt buộc sau khi đổi env).

## 7. OAuth (Google / Facebook)

Redirect URI trỏ **Railway backend**:

```
https://synctube-be-production.up.railway.app/login/oauth2/code/google
https://synctube-be-production.up.railway.app/login/oauth2/code/facebook
```

## 8. Kiểm tra

- `https://your-be.up.railway.app/api/auth/me` → 401/403 (OK, cần token)
- Mở link Vercel → Đăng nhập Google → vào trang chủ
- Tạo phòng, test sync 2 tab

## Xử lý sự cố

| Lỗi | Cách xử lý |
|-----|------------|
| Build fail Java | Dockerfile dùng Java 21 — xem Deploy Logs |
| DB connection fail | Kiểm tra `MYSQL*` references đã link đúng service MySQL |
| CORS | `FRONTEND_URL` khớp URL Vercel (không slash cuối) |
| OAuth redirect mismatch | Redirect URI khớp chính xác domain Railway |
| WebSocket fail | Dùng `wss://` (HTTPS), không `ws://` |

## Chi phí

Railway có credit miễn phí ban đầu, sau đó ~$5/tháng tùy usage (BE + MySQL).
