import { PropertyValues } from "lit";
import { customElement, state } from "lit/decorators.js";

import { ArchDataTable } from "../../archDataTable/index";
import { Dataset, ProcessingState } from "../../lib/types";
import { Paths } from "../../lib/helpers";
import Styles from "./styles";

@customElement("arch-dataset-explorer-table")
export class ArchDatasetExplorerTable extends ArchDataTable<Dataset> {
  @state() columnNameHeaderTooltipMap = {
    category:
      "Dataset categories are Collection, Network, Text, and File Format",
    sample:
      "Sample datasets contain only the first 100 available records from a collection",
  };

  static styles = [...ArchDataTable.styles, ...Styles];

  willUpdate(_changedProperties: PropertyValues) {
    super.willUpdate(_changedProperties);

    this.apiCollectionEndpoint = "/datasets";
    this.apiItemResponseIsArray = true;
    this.apiItemTemplate =
      "/datasets?collectionId=:collectionId&job=:jobId&sample=:isSample";
    this.itemPollPredicate = (item) => item.state === ProcessingState.Running;
    this.itemPollPeriodSeconds = 3;
    this.apiStaticParamPairs = [["state!", ProcessingState.NotStarted]];
    this.cellRenderers = [
      (name, dataset) =>
        dataset.state !== ProcessingState.Finished
          ? `${dataset.name}`
          : `<a href="${Paths.dataset(dataset.id, dataset.sample)}">
               <span class="highlightable">${dataset.name}</span>
            </a>`,
      undefined,
      (collectionName, dataset) =>
        `<a href="${Paths.collection(dataset.collectionId)}">
           <span class="highlightable">${collectionName as string}</span>
        </a>`,
      (sample) => ((sample as Dataset["sample"]) === -1 ? "No" : "Yes"),
      undefined,
      (startTime) => (startTime as string)?.slice(0, -3),
      (finishedTime, dataset) =>
        dataset.state === ProcessingState.Running
          ? ""
          : (finishedTime as string)?.slice(0, -3),
    ];
    this.columnFilterDisplayMaps = [
      undefined,
      undefined,
      undefined,
      { 100: "Yes", [-1]: "No" },
    ];
    this.columns = [
      "name",
      "category",
      "collectionName",
      "sample",
      "state",
      "startTime",
      "finishedTime",
      "numFiles",
    ];
    this.columnHeaders = [
      "Dataset",
      "Category",
      "Collection",
      "Sample",
      "State",
      "Started",
      "Finished",
      "Files",
    ];
    this.filterableColumns = [
      true,
      true,
      true,
      true,
      true,
      false,
      false,
      false,
    ];
    this.searchColumns = ["name", "category", "collectionName", "state"];
    this.searchColumnLabels = ["Name", "Category", "Collection", "State"];
    this.singleName = "Dataset";
    this.sort = "-startTime";
    this.sortableColumns = [true, true, true, true, true, true, true, true];
    this.persistSearchStateInUrl = true;
    this.pluralName = "Datasets";
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-dataset-explorer-table": ArchDatasetExplorerTable;
  }
}
