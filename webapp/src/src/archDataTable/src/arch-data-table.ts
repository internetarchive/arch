import { PropertyValues } from "lit";
import { customElement, state } from "lit/decorators.js";

import { htmlAttrEscape } from "../../lib/webservices/src/lib/helpers";
import { AitDataTable } from "../../lib/webservices/src/aitDataTable/index";

import API from "../../lib/ait-data-table-api-adapter";
import Styles from "./styles";

import "../../archLoadingIndicator/index";
import "../../archHoverTooltip/index";

@customElement("arch-data-table")
export class ArchDataTable<RowT> extends AitDataTable<RowT> {
  @state() columnNameHeaderTooltipMap: Record<string, string> = {};

  // Apply any ARCH-specific styles.
  static styles = [...AitDataTable.styles, ...Styles];

  constructor() {
    super();
    this.apiFactory = API;
    this.loadingMessage = "<arch-loading-indicator></arch-loading-indicator>";
    this.pageLength = 10;
  }

  addHeaderTooltip(headerName: string, text: string) {
    const headerTr = this.dataTable.querySelector(
      "thead > tr"
    ) as HTMLTableRowElement;
    const el = headerTr.querySelector(
      `th.${headerName} > button`
    ) as HTMLButtonElement;
    if (!el) {
      console.warn(`Could not add tooltip to header: ${headerName}`);
      return;
    }
    el.innerHTML = `
      <arch-hover-tooltip
        style="display: inline-block; color: #fff;"
        text="${htmlAttrEscape(text)}"
      >
        ${el.textContent as string}
      </arch-hover-tooltip>
    `;
  }

  firstUpdated(_changedProperties: PropertyValues) {
    super.firstUpdated(_changedProperties);
    // Add hover tooltips after DataTable has been rendered.
    for (const [columnName, text] of Object.entries(
      this.columnNameHeaderTooltipMap
    )) {
      this.addHeaderTooltip(columnName, text);
    }
  }
}
