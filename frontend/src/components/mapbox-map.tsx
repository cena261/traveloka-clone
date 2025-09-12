"use client";
import mapboxgl from "mapbox-gl";
import { useEffect, useRef } from "react";

mapboxgl.accessToken = process.env.NEXT_PUBLIC_MAPBOX_TOKEN ?? "";

type Props = { lat?: number; lng?: number; zoom?: number };

export default function MapboxMap({ lat = 10.776, lng = 106.700, zoom = 10 }: Props) {
  const mapRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!mapRef.current || !mapboxgl.accessToken) return;
    const map = new mapboxgl.Map({
      container: mapRef.current,
      style: "mapbox://styles/mapbox/streets-v12",
      center: [lng, lat],
      zoom,
    });
    new mapboxgl.Marker().setLngLat([lng, lat]).addTo(map);
    return () => map.remove();
  }, [lat, lng, zoom]);

  return <div ref={mapRef} className="w-full h-[400px] rounded-2xl" />;
}
