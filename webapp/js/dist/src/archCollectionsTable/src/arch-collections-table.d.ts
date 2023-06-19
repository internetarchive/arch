import { PropertyValues } from "lit";
import { ArchDataTable } from "../../archDataTable/index";
import { Collection } from "../../lib/types";
export declare class ArchCollectionsTable extends ArchDataTable<Collection> {
    static styles: import("lit").CSSResult[];
    willUpdate(_changedProperties: PropertyValues): void;
    postSelectionChangeHandler(selectedRows: Array<Collection>): void;
    selectionActionHandler(action: string, selectedRows: Array<Collection>): void;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-collections-table": ArchCollectionsTable;
    }
}
