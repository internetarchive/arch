import { customElement } from "lit/decorators.js";

import { AitDataTable } from "../../lib/webservices/src/aitDataTable/index";
import API from "../../lib/ait-data-table-api-adapter";
import Styles from "./styles";

import "../../archLoadingIndicator/index";

@customElement("arch-data-table")
export class ArchDataTable<RowT> extends AitDataTable<RowT> {
  // Apply any ARCH-specific styles.
  static styles = [...AitDataTable.styles, ...Styles];

  constructor() {
    super();
    this.apiFactory = API;
    this.loadingMessage = "<arch-loading-indicator></arch-loading-indicator>";
    this.pageLength = 10;
  }
}
