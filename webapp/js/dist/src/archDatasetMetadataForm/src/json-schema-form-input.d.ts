import { LitElement } from "lit";
import { PublishedDatasetMetadataJSONSchema } from "../../lib/types";
export declare class JsonSchemaFormInput extends LitElement {
    name: string;
    schema: PublishedDatasetMetadataJSONSchema["properties"];
    value: string;
    label: string;
    static styles: import("lit").CSSResult[];
    render(): import("lit-html").TemplateResult<1>;
}
declare global {
    interface HTMLElementTagNameMap {
        "json-schema-form-input": JsonSchemaFormInput;
    }
}
