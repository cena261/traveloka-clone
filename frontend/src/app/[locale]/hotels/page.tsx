"use client";
import { useHotels } from "@/features/hotel/api/use-hotels";

export default function HotelsPage() {
  const { data, isLoading } = useHotels({ city: "Ho Chi Minh", page: 1 });
  if (isLoading) return <div className="p-6">Loading...</div>;
  return (
    <ul className="p-6 space-y-2">
      {data?.map(h => (
        <li key={h.id} className="border rounded-xl p-4">
          <div className="font-medium">{h.name}</div>
          <div className="text-sm text-muted-foreground">{h.city}</div>
        </li>
      ))}
    </ul>
  );
}
