import { LitElement, PropertyValues } from "lit";
import { PublishedDatasetMetadata, PublishedDatasetMetadataJSONSchema } from "../../lib/types";
export declare class ArchDatasetMetadataForm extends LitElement {
    metadata: undefined | PublishedDatasetMetadata;
    form: HTMLFormElement;
    inputs: NodeList;
    static styles: import("lit").CSSResult[];
    static schema: PublishedDatasetMetadataJSONSchema;
    static validator: import("ajv").ValidateFunction<PublishedDatasetMetadata>;
    static propertiesOrder: string[];
    static orderedMetadataKeys: (keyof PublishedDatasetMetadata)[];
    private _propToInput;
    render(): import("lit-html").TemplateResult<1>;
    willUpdate(changedProperties: PropertyValues<this>): void;
    updated(): void;
    private _addFieldSelectHandler;
    private _addMetadataValue;
    private _updateMetadataValue;
    private _removeMetadataValue;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-dataset-metadata-form": ArchDatasetMetadataForm;
    }
}
