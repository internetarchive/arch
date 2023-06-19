export type ValueOf<T> = T[keyof T];
export declare function htmlAttrEscape(s: string): string;
export declare function humanBytes(bytes: number, decPlaces?: number): string;
export declare function toTitleCase(text: string): string;
export declare function isoStringToDateString(timestamp: string | Date, includeTime?: boolean): string;
