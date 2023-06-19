import { LitElement } from "lit";
import { PublishedDatasetMetadataJSONSchema } from "../../lib/types";
import "./json-schema-form-input";
export declare class JsonSchemaFormInputBlock extends LitElement {
    name: string;
    schema: PublishedDatasetMetadataJSONSchema["properties"];
    values: Array<string>;
    label: string;
    static styles: import("lit").CSSResult[];
    render(): import("lit-html").TemplateResult<1>;
}
declare global {
    interface HTMLElementTagNameMap {
        "json-schema-form-input-block": JsonSchemaFormInputBlock;
    }
}
