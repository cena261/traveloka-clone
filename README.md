# Traveloka Clone – Full-stack Overview

Dự án thập cẩm, có gì xài đó. Test tất cả công nghệ đã, đang và chưa học :D

---

## 🖥️ Frontend

- **Next.js (React + TypeScript)**
- **Tailwind CSS + shadcn/ui**
- **TanStack Query (React Query)**
- **next-intl (vi/en)**
- **Map Integration**
  - **Google Places API**
  - **Mapbox GL JS**

---

## ⚙️ Backend (Monolithic)

- **Java 21 + Spring Boot 3**
- **Flyway**
- **Spring Security + Keycloak Adapter**
- **Spring Data Redis**
- **Spring Mail (dev: MailHog)**
- **MinIO SDK**
- **Spring Data Elasticsearch**

---

## 🗄️ Database & Storage

- **PostgreSQL**
- **Redis**
- **MinIO (S3-compatible)**

---

## 🔐 Identity & Security

- **Keycloak (OAuth2/OIDC)**
- **...**
- 
---

## 🧰 Dev & Ops

- **...**

---

## 🧪 Testing 

- **Frontend**: Vitest/Jest + Testing Library; Playwright E2E.
- **Backend**: JUnit 5 + Testcontainers (Postgres/Redis/ES).
- **Contract**: OpenAPI và/hoặc tests cho adapters (ES/Keycloak/MinIO).

---

## 🗺️ Tính năng chính 

- Tìm kiếm & lọc nâng cao (ES + Redis cache).
- Autocomplete địa điểm (Google Places) + bản đồ trực quan (Mapbox).
- Đa ngôn ngữ vi/en (next-intl).
- Đăng nhập/đăng ký, quên mật khẩu. Quản lý thông qua Keycloak.
- Quản lý booking, thanh toán (mở rộng gateway sau, VNPAY hoặc MOMO).
- Quản trị nội dung (upload ảnh qua MinIO, presigned URL).

