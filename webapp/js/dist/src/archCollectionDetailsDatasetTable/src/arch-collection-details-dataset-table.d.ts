import { PropertyValues } from "lit";
import { ArchDataTable } from "../../archDataTable/index";
import { Dataset } from "../../lib/types";
export declare class ArchCollectionDetailsDatasetTable extends ArchDataTable<Dataset> {
    collectionId: string;
    static styles: import("lit").CSSResult[];
    willUpdate(_changedProperties: PropertyValues): void;
    nonSelectionActionHandler(action: string): void;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-collection-details-dataset-table": ArchCollectionDetailsDatasetTable;
    }
}
