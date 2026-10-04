import { createElement as h } from "react";
import { safeWeb, categories, type Facility } from "./nearby-client.ts";

export function NearbyResults({ results }: { results: Facility[] }) {
  const link = (text: string, url: string | null) =>
    safeWeb(url)
      ? h(
          "a",
          {
            href: safeWeb(url),
            target: "_blank",
            rel: "noopener noreferrer",
            className: "underline text-primary",
          },
          text
        )
      : null;

  return h(
    "section",
    {
      "aria-label": "Places results",
      className: "mt-8 border-t",
    },

    h(
      "p",
      {
        translate: "no",
        className: "my-4 text-sm font-normal text-[#5e5e5e]",
      },
      "Nearby place data © OpenStreetMap contributors"
    ),

    h(
      "p",
      {
        className: "text-sm text-muted-foreground",
      },
      "Up to 20 provider results ranked by distance. Distances are approximate straight-line distances, not driving distances. Confirm facility details before visiting."
    ),

    results.length === 0
      ? h(
          "p",
          {
            role: "status",
            className: "py-8",
          },
          "No facilities found. Try another category or a larger radius."
        )
      : results.map((f) =>
          h(
            "article",
            {
              key: f.providerId,
              className: "border-b py-6 space-y-3",
            },

            h(
              "h2",
              {
                className: "text-xl font-semibold break-words",
              },
              f.name
            ),

            h(
              "p",
              null,
              categories[f.category],
              f.distanceMeters === null
                ? ""
                : ` · ${(f.distanceMeters / 1000).toFixed(1)} km approximately`
            ),

            f.address && h("p", null, f.address),

            h(
              "p",
              null,
              f.openNow === null
                ? "Open/closed status unavailable"
                : f.openNow
                  ? "Open now (provider supplied)"
                  : "Closed now (provider supplied)"
            ),

            f.hours.length
              ? h(
                  "details",
                  null,
                  h("summary", null, "Opening hours"),
                  f.hours.map((x, i) => h("p", { key: i }, x))
                )
              : h("p", null, "Hours unavailable"),

            h(
              "div",
              {
                className: "flex flex-wrap gap-5",
              },

              f.phone && /^\+?[0-9]{5,18}$/.test(f.phone)
                ? h(
                    "a",
                    {
                      href: `tel:${f.phone}`,
                      className: "underline",
                    },
                    `Call ${f.phone}`
                  )
                : null,

              link("Official website", f.website),
              link("Directions", f.directions),
              link("Book externally", f.bookingUrl)
            ),

            f.attributions.map((a, i) =>
              h(
                "p",
                {
                  key: i,
                  className: "text-sm",
                },
                "Source: ",
                link(a.name, a.url) ?? a.name
              )
            )
          )
        )
  );
}