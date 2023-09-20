import { PropertyValues } from "lit";
import { ArchDataTable } from "../../archDataTable/index";
import { Dataset } from "../../lib/types";
export declare class ArchDatasetExplorerTable extends ArchDataTable<Dataset> {
    columnNameHeaderTooltipMap: {
        category: string;
        sample: string;
    };
    static styles: import("lit").CSSResult[];
    willUpdate(_changedProperties: PropertyValues): void;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-dataset-explorer-table": ArchDatasetExplorerTable;
    }
}
