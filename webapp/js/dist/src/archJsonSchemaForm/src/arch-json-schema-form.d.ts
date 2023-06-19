import { LitElement } from "lit";
export declare class ArchJsonSchemaForm extends LitElement {
    schemaUrl: URL;
    static styles: import("lit").CSSResult[];
    render(): void;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-json-schema-form": ArchJsonSchemaForm;
    }
}
