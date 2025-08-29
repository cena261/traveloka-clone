import createMiddleware from "next-intl/middleware";

export default createMiddleware({
  locales: ["vi", "en"],
  defaultLocale: "vi",
});

export const config = {
  matcher: ["/", "/(vi|en)/:path*"],
};
