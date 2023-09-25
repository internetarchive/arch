import { LitElement } from "lit";
import { Collection } from "../../lib/types";
import "../../archAlert/index";
export declare class ArchSubCollectionBuilder extends LitElement {
    collections: Array<Collection>;
    sourceCollectionIds: Set<Collection["id"]>;
    form: HTMLFormElement;
    sourceSelect: HTMLSelectElement;
    static styles: import("lit").CSSResult[];
    static urlCollectionsParamName: string;
    connectedCallback(): Promise<void>;
    render(): import("lit-html").TemplateResult<1>;
    private inputHandler;
    private initCollections;
    private setSourceCollectionIdsUrlParam;
    private sourceCollectionsChangeHandler;
    private static fieldValueParserMap;
    private static fieldValueValidatorMessagePairMap;
    private static decodeFormDataValue;
    private static validateDecodedFormData;
    private get formData();
    private setFormInputValidity;
    private doPost;
    private createSubCollection;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-sub-collection-builder": ArchSubCollectionBuilder;
    }
}
