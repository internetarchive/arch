import { LitElement } from "lit";
import { Dataset } from "../../lib/types";
import "../../archCard/index";
import "../../archLoadingIndicator/index";
export declare class ArchRecentDatasetsCard extends LitElement {
    numTotalDatasets: number;
    datasets: undefined | Array<Dataset>;
    static maxDisplayedDatasets: number;
    static styles: import("lit").CSSResult[];
    constructor();
    render(): import("lit-html").TemplateResult<1>;
    private initDatasets;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-recent-datasets-card": ArchRecentDatasetsCard;
    }
}
