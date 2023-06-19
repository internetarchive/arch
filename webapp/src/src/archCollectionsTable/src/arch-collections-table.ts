import { PropertyValues } from "lit";
import { customElement } from "lit/decorators.js";

import { ArchDataTable } from "../../archDataTable/index";
import { Collection } from "../../lib/types";
import { Topics } from "../../lib/pubsub";
import {
  Paths,
  humanBytes,
  htmlAttrEscape as _,
  isoStringToDateString,
} from "../../lib/helpers";
import Styles from "./styles";

@customElement("arch-collections-table")
export class ArchCollectionsTable extends ArchDataTable<Collection> {
  static styles = [...ArchDataTable.styles, ...Styles];

  willUpdate(_changedProperties: PropertyValues) {
    super.willUpdate(_changedProperties);

    this.actionButtonLabels = ["Generate Dataset", "Create Custom Collection"];
    this.actionButtonSignals = [
      Topics.GENERATE_DATASET,
      Topics.CREATE_SUB_COLLECTION,
    ];
    this.apiCollectionEndpoint = "/collections";

    /* eslint-disable @typescript-eslint/restrict-template-expressions */
    this.cellRenderers = [
      (name, collection: Collection) => `
        <a href="/collections/${_(collection.id)}" title="${_(
        name as string
      )}">${name}</a>
      `,
      { true: "Yes", false: "No" },
      (lastJobName, collection: Collection) => {
        if (lastJobName === null) {
          return "";
        }
        lastJobName = lastJobName as string;
        const lastJobId = collection.lastJobId as string;
        const lastJobSample = collection.lastJobSample as boolean;
        // Convert the lastJobSample boolean to a Dataset.sample integer.
        const datasetSample = lastJobSample ? 1 : -1;
        // NOTE - this manual Dataset ID string composition is brittle
        return `
          <a href="${Paths.dataset(
            `${collection.id}:${lastJobId}`,
            datasetSample
          )}" title="${_(lastJobName)}">
            ${lastJobName}
          </a>
        `;
      },
      (lastJobTime) =>
        !lastJobTime ? "" : isoStringToDateString(lastJobTime as string),
      (_, collection: Collection) => {
        return humanBytes(
          collection.sortSize === -1 ? 0 : collection.sortSize,
          1
        );
      },
    ];
    /* eslint-enable @typescript-eslint/restrict-template-expressions */

    this.columns = ["name", "public", "lastJobName", "lastJobTime", "sortSize"];
    this.columnHeaders = [
      "Name",
      "Public",
      "Latest Dataset",
      "Dataset Date",
      "Size",
    ];
    this.selectable = true;
    this.singleName = "Collection";
    this.pluralName = "Collections";
  }

  postSelectionChangeHandler(selectedRows: Array<Collection>) {
    /* Update DataTable.actionButtonDisabled based on the number
       of selected rows.
    */
    const { dataTable } = this;
    const { props } = dataTable;
    const numSelected = selectedRows.length;
    const generateDatasetEnabled = numSelected === 1;
    const createSubCollectionEnabled = true;
    props.actionButtonDisabled = [
      !generateDatasetEnabled,
      !createSubCollectionEnabled,
    ];
    dataTable.setSelectionActionButtonDisabledState(numSelected === 0);
  }

  selectionActionHandler(action: string, selectedRows: Array<Collection>) {
    switch (action) {
      case Topics.GENERATE_DATASET:
        window.location.href = Paths.generateCollectionDataset(
          selectedRows[0].id
        );
        break;
      case Topics.CREATE_SUB_COLLECTION:
        window.location.href = Paths.buildSubCollection(
          selectedRows.map((x) => x.id)
        );
    }
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-collections-table": ArchCollectionsTable;
  }
}
