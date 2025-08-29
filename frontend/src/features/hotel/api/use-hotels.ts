import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { http } from "@/lib/http";

export type Hotel = { id: string; name: string; city: string };

export function useHotels(params?: { city?: string; page?: number }) {
  return useQuery<Hotel[]>({
    queryKey: ["hotels", params],
    queryFn: async () => {
      const res = await http.get<Hotel[]>("/hotels", { params });
      return res.data;
    },
    placeholderData: keepPreviousData,
    staleTime: 30_000,
    retry: 1,
    refetchOnWindowFocus: false,
  });
}
