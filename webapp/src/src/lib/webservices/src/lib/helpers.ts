// https://stackoverflow.com/a/49286056
export type ValueOf<T> = T[keyof T];

// Escape a string for inclusion as an element attribute value.
export function htmlAttrEscape(s: string): string {
  return s.replace(/"/g, "&quot;");
}

export function humanBytes(bytes: number, decPlaces = 3): string {
  for (const prefix of ["", "Ki", "Mi", "Gi", "Ti", "Pi"]) {
    if (bytes < 1024) {
      const [whole, frac] = bytes.toString().split(".");
      return `${whole}${frac ? `.${frac.slice(0, decPlaces)}` : ""} ${prefix}B`;
    }
    bytes /= 1024;
  }
  return "";
}

export function toTitleCase(text: string): string {
  if (text.includes("_")) {
    text = text.replaceAll("_", " ");
  }
  const textArray = text.split(" ");
  // Eg: pre_deposit_modified_at ---> Pre Deposit Modified At
  return textArray
    .map((word) => {
      return word[0].toUpperCase() + word.slice(1, word.length);
    })
    .join(" ");
}

export function isoStringToDateString(
  timestamp: string | Date,
  includeTime = false
): string {
  const dayConfig: Intl.DateTimeFormatOptions = {
    month: "short",
    day: "numeric",
    year: "numeric",
  };
  const timeConfig: Intl.DateTimeFormatOptions = {
    hour: "numeric",
    minute: "2-digit",
    timeZoneName: "short",
  };
  return includeTime
    ? new Date(timestamp).toLocaleTimeString(navigator.language, {
        ...dayConfig,
        ...timeConfig,
      })
    : new Date(timestamp).toLocaleDateString(navigator.language, dayConfig);
}
