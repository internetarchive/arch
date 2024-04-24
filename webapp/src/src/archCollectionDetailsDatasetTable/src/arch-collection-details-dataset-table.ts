import { PropertyValues } from "lit";
import { customElement, property, state } from "lit/decorators.js";

import { ArchDataTable } from "../../archDataTable/index";
import { Dataset } from "../../lib/types";
import { Topics } from "../../lib/pubsub";
import { Paths } from "../../lib/helpers";
import { ProcessingState } from "../../lib/types";
import Styles from "./styles";

@customElement("arch-collection-details-dataset-table")
export class ArchCollectionDetailsDatasetTable extends ArchDataTable<Dataset> {
  @property({ type: String }) collectionId!: string;

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
    this.apiStaticParamPairs = [
      ["collectionId", this.collectionId],
      ["state!", ProcessingState.NotStarted],
    ];
    this.cellRenderers = [
      (name, dataset) =>
        dataset.state !== ProcessingState.Finished
          ? `${name as string}`
          : `<a href="${Paths.dataset(dataset.id, dataset.sample)}">${
              name as string
            }</a>`,
      undefined,
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
      { 100: "Yes", [-1]: "No" },
    ];
    this.columns = [
      "name",
      "category",
      "sample",
      "state",
      "startTime",
      "finishedTime",
      "numFiles",
    ];
    this.columnHeaders = [
      "Dataset",
      "Category",
      "Sample",
      "State",
      "Started",
      "Finished",
      "Files",
    ];
    this.filterableColumns = [true, true, true, true, false, false, false];
    this.nonSelectionActionLabels = ["Generate a New Dataset"];
    this.nonSelectionActions = [Topics.GENERATE_DATASET];
    this.singleName = "Dataset";
    this.sort = "-startTime";
    this.sortableColumns = [true, true, true, true, true, true, true];
    this.pluralName = "Datasets";
  }

  nonSelectionActionHandler(action: string) {
    switch (action) {
      case Topics.GENERATE_DATASET:
        window.location.href = Paths.generateCollectionDataset(
          this.collectionId
        );
        break;
      default:
        break;
    }
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-collection-details-dataset-table": ArchCollectionDetailsDatasetTable;
  }
}
