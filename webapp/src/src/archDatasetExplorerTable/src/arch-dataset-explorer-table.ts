import { PropertyValues } from "lit";
import { customElement } from "lit/decorators.js";

import { ArchDataTable } from "../../archDataTable/index";
import { Dataset, ProcessingState } from "../../lib/types";
import { Paths } from "../../lib/helpers";
import Styles from "./styles";

@customElement("arch-dataset-explorer-table")
export class ArchDatasetExplorerTable extends ArchDataTable<Dataset> {
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
          : `<a href="${Paths.dataset(dataset.id, dataset.sample)}">${
              dataset.name
            }</a>`,
      undefined,
      (collectionName, dataset) =>
        `<a href="${Paths.collection(dataset.collectionId)}">${
          collectionName as string
        }</a>`,
      (sample) => ((sample as Dataset["sample"]) === -1 ? "No" : "Yes"),
      undefined,
      (startTime) => (startTime as string)?.slice(0, -3),
      (finishedTime, dataset) =>
        dataset.state === ProcessingState.Running
          ? ""
          : (finishedTime as string)?.slice(0, -3),
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
    this.singleName = "Dataset";
    this.pluralName = "Datasets";
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-dataset-explorer-table": ArchDatasetExplorerTable;
  }
}
