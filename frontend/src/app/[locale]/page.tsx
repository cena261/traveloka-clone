"use client";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";

export default function HomePage() {
  const t = useTranslations();
  return (
    <main className="p-6">
      <h1 className="text-2xl font-semibold mb-4">{t("home.title")}</h1>
      <Button>{t("common.bookNow")}</Button>
    </main>
  );
}
