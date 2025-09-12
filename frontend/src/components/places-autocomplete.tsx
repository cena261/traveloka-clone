"use client";
import { useEffect, useRef } from "react";

type Props = { onSelect: (place: google.maps.places.PlaceResult) => void };

export default function PlacesAutocomplete({ onSelect }: Props) {
  const inputRef = useRef<HTMLInputElement | null>(null);
  useEffect(() => {
    if (!inputRef.current || !(window as any).google) return;
    const ac = new google.maps.places.Autocomplete(inputRef.current!, {
      types: ["(cities)"], 
      fields: ["formatted_address", "geometry", "name", "place_id"],
    });
    ac.addListener("place_changed", () => {
      const place = ac.getPlace();
      onSelect(place);
    });
  }, []);
  return (
    <input
      ref={inputRef}
      placeholder="Bạn muốn đi đâu?"
      className="w-full border rounded-xl px-4 py-2"
    />
  );
}
