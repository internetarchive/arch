import { TemplateResult, html } from "lit";
import { isoStringToDateString } from "./helpers";

export function humanBytes(bytes: number, decPlaces = 3): string | undefined {
  for (const prefix of ["", "Ki", "Mi", "Gi", "Ti", "Pi"]) {
    if (bytes < 1024) {
      const [whole, frac] = bytes.toString().split(".");
      return `${whole}${frac ? `.${frac.slice(0, decPlaces)}` : ""} ${prefix}B`;
    }
    bytes /= 1024;
  }
}

// Join URL path parts into a string.
export const joinPath = (...xs: string[]) =>
  `${xs[0].startsWith("/") ? "/" : ""}` +
  `${xs
    .flatMap((x) => x.split("/"))
    .filter((x) => x !== "")
    .join("/")}` +
  `${xs.at(-1)?.endsWith("/") ? "/" : ""}`;

// TODO
// Replace calls to formatDate with calls to isoStringToDateString
// Formatted Date
export function formatDate(date: Date | string, includeTime = true): string {
  const _formattedDate = isoStringToDateString(date, includeTime);
  return _formattedDate === "Invalid Date" ? "" : _formattedDate;
}

// Takes a string and returns an HTML string that breaks at:
// underscores "_"
// hyphens "-"
// periods "."
// Useful for displaying long filenames
export function breakableString(
  value: string | undefined
): TemplateResult | null {
  return value === undefined
    ? null
    : html`
        ${value.split(/(_|-|\.)/).map((value, index) => {
          return index % 2 ? html`<wbr />${value}` : html`${value}`;
        })}
      `;
}

export function reportTypeNiceName(reportType?: string): string {
  return reportType
    ? `${reportType[0].toUpperCase()}${reportType.slice(1).toLowerCase()}`
    : "";
}
