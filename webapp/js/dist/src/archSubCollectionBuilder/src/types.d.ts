type FormFieldName = "sources" | "name" | "surtPrefixesOR" | "timestampFrom" | "timestampTo" | "statusPrefixesOR" | "mimesOR";
export type DecodedFormData = Record<FormFieldName, string | Array<string> | Error>;
export {};
