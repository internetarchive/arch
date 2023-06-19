import { LitElement } from "lit";
import { Collection } from "../../lib/types";
import "../../archCard/index";
import "../../archLoadingIndicator/index";
export declare class ArchCollectionsCard extends LitElement {
    collections: undefined | Array<Collection>;
    collectionDatasetCounts: undefined | Record<Collection["id"], number>;
    static numDisplayedCollections: number;
    static styles: import("lit").CSSResult[];
    constructor();
    render(): import("lit-html").TemplateResult<1>;
    private initCollections;
    private initCollectionDatasetCounts;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-collections-card": ArchCollectionsCard;
    }
}
