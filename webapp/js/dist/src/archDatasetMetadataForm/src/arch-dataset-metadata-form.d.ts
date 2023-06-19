import { LitElement } from "lit";
import { PublishedDatasetMetadata, PublishedDatasetMetadataJSONSchema, PublishedDatasetMetadataKeys, PublishedDatasetMetadataValue } from "../../lib/types";
export declare class ArchDatasetMetadataForm extends LitElement {
    metadata: PublishedDatasetMetadata;
    form: HTMLFormElement;
    inputs: NodeList;
    static styles: import("lit").CSSResult[];
    static schema: PublishedDatasetMetadataJSONSchema;
    static validator: import("ajv").ValidateFunction<{
        creator?: PublishedDatasetMetadataValue | undefined;
        description?: PublishedDatasetMetadataValue | undefined;
        licenseurl?: PublishedDatasetMetadataValue | undefined;
        subject?: PublishedDatasetMetadataValue | undefined;
        title?: PublishedDatasetMetadataValue | undefined;
    }>;
    static orderedMetadataKeys: PublishedDatasetMetadataKeys[];
    private _propToInput;
    render(): import("lit-html").TemplateResult<1>;
    willUpdate(changedProperties: Map<PropertyKey, undefined>): void;
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
