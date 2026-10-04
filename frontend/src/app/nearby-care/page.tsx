"use client";

import { useEffect, useState } from "react";
import {
  nearby,
  categories,
  requestLocation,
  type Category,
  type Facility,
} from "@/lib/nearby-client";
import { NearbyResults } from "@/lib/nearby-view";
import { Button } from "@/components/ui/button";

export default function NearbyCarePage() {
  const [available, setAvailable] = useState<boolean | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const [latitude, setLatitude] = useState("");
  const [longitude, setLongitude] = useState("");
  const [category, setCategory] = useState<Category>("HOSPITAL");
  const [radius, setRadius] = useState(5000);
  const [results, setResults] = useState<Facility[] | null>(null);

  function check() {
    setError("");
    nearby
      .config()
      .then((x) => setAvailable(x.available))
      .catch(() =>
        setError("Unable to check provider configuration. Retry.")
      );
  }

  useEffect(() => {
    nearby
      .config()
      .then((x) => setAvailable(x.available))
      .catch(() =>
        setError("Unable to check provider configuration. Retry.")
      );
  }, []);

  async function locate() {
    setBusy(true);
    setError("");

    try {
      const x = await requestLocation(navigator.geolocation);
      setLatitude(String(x.latitude));
      setLongitude(String(x.longitude));
      setResults(null);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function search(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setResults(null);

    try {
      const x = await nearby.search(
        Number(latitude),
        Number(longitude),
        radius,
        category
      );
      setResults(x.results);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="text-3xl font-semibold">Nearby Care</h1>

      <p className="mt-3 text-muted-foreground">
        Find healthcare services and contact them directly. CarePath does not
        offer appointment slots.
      </p>

      <p className="my-5">
        Location is optional. Use your location once, or enter coordinates
        manually. Searching sends these coordinates and your chosen category
        through CarePath to OpenStreetMap-based services. CarePath does not
        save your location or send your medical records.
      </p>

      <p className="mb-5 text-sm">
        Nearby place data © OpenStreetMap contributors.
      </p>

      {error && (
        <p role="alert" className="my-4 text-red-900">
          {error}
        </p>
      )}

      {available === null && !error && (
        <p role="status">Checking provider availability...</p>
      )}

      {available === false && (
        <p role="status" className="my-5">
          Nearby Care is temporarily unavailable. Your other CarePath features
          remain available.
        </p>
      )}

      <Button variant="outline" onClick={check} disabled={busy}>
        Retry availability check
      </Button>

      {available && (
        <form onSubmit={search} className="mt-6 space-y-5">
          <Button type="button" onClick={locate} disabled={busy}>
            Use my location
          </Button>

          <fieldset className="grid gap-4 sm:grid-cols-2" disabled={busy}>
            <legend className="mb-3">
              Search location (decimal coordinates)
            </legend>

            <label>
              Latitude
              <input
                className="mt-1 block w-full rounded border p-2"
                type="number"
                step="any"
                min="-90"
                max="90"
                required
                value={latitude}
                onChange={(e) => setLatitude(e.target.value)}
              />
            </label>

            <label>
              Longitude
              <input
                className="mt-1 block w-full rounded border p-2"
                type="number"
                step="any"
                min="-180"
                max="180"
                required
                value={longitude}
                onChange={(e) => setLongitude(e.target.value)}
              />
            </label>

            <label>
              Category
              <select
                className="mt-1 block w-full rounded border p-2"
                value={category}
                onChange={(e) => setCategory(e.target.value as Category)}
              >
                {Object.entries(categories).map(([key, label]) => (
                  <option key={key} value={key}>
                    {label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Search radius
              <select
                className="mt-1 block w-full rounded border p-2"
                value={radius}
                onChange={(e) => setRadius(Number(e.target.value))}
              >
                {[1000, 5000, 10000, 20000].map((x) => (
                  <option key={x} value={x}>
                    {x / 1000} km
                  </option>
                ))}
              </select>
            </label>
          </fieldset>

          <Button type="submit" disabled={busy}>
            {busy ? "Working..." : "Search with this location"}
          </Button>
        </form>
      )}

      {busy && (
        <p role="status" aria-live="polite" className="my-5">
          Please wait...
        </p>
      )}

      {results !== null && <NearbyResults results={results} />}
    </div>
  );
}