import axios from "axios";

export const http = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api",
  withCredentials: true,
});

http.interceptors.response.use(
  (res) => res,
  (err) => {
    // ví dụ: xử lý 401 / refresh token ở đây
    return Promise.reject(err);
  }
);
