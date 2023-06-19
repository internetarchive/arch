import { LitElement } from "lit";
import { Collection } from "../../lib/types";
import "../../archAlert/index";
export declare class ArchSubCollectionBuilder extends LitElement {
    collections: Array<Collection>;
    sourceCollectionIds: Set<Collection["id"]>;
    form: HTMLFormElement;
    sourceSelect: HTMLSelectElement;
    static urlCollectionsParamName: string;
    createRenderRoot(): this;
    connectedCallback(): Promise<void>;
    private get _formData();
    render(): import("lit-html").TemplateResult<1>;
    private initCollections;
    private setSourceCollectionIdsUrlParam;
    private sourceCollectionsChangeHandler;
    private createSubCollection;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-sub-collection-builder": ArchSubCollectionBuilder;
    }
}
