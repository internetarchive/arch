import { TemplateResult } from "lit";
export declare function humanBytes(bytes: number, decPlaces?: number): string | undefined;
export declare const joinPath: (...xs: string[]) => string;
export declare function formatDate(date: Date | string, includeTime?: boolean): string;
export declare function breakableString(value: string | undefined): TemplateResult | null;
export declare function reportTypeNiceName(reportType?: string): string;
