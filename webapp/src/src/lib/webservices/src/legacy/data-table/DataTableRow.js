import DataTableCell from "./DataTableCell.js";

import { customElementsMaybeDefine } from "../lib/domLib.js";

export default class DataTableRow extends HTMLTableRowElement {
  constructor() {
    super();
    // Create a promise that will be resolved at the end of connectedCallback.
    this.connectedPromise = new Promise((resolve) => {
      this.connectedResolver = resolve;
    });
  }

  async connectedCallback() {
    const {
      cellRenderers,
      columns,
      data,
      linkFormats,
      linkTargets,
      nullString,
      selectable,
      selectionDisabled,
      selected,
    } = this.props;

    // Add a checkbox cell if selectable.
    if (selectable) {
      this.innerHTML = `
        <td class="select">
          <input type="checkbox" ${
            selected ? "checked" : ""
          } aria-label="Select Row">
        </td>
      `;
    }
    const checkbox = this.querySelector(":scope > td.select > input");

    this.refs = { checkbox };

    // Disable and hide the selection checkbox if selection is disabled.
    if (selectionDisabled) {
      checkbox.disabled = true;
      checkbox.style.visibility = "hidden";
    }

    let colSpanSkipRemaining = 0;
    let colIdx = 0;
    for await (const columnName of columns) {
      // Maybe skip this column.
      if (colSpanSkipRemaining) {
        colSpanSkipRemaining -= 1;
        colIdx += 1;
        continue; // eslint-disable-line no-continue
      }

      const dataTableCell = new DataTableCell();
      dataTableCell.props = {
        columnName,
        linkFormat: linkFormats[colIdx],
        linkTarget: linkTargets[colIdx],
        nullString,
        rowData: data,
        renderer: cellRenderers[colIdx],
      };

      // Append the cell and wait for connectedCallack() to actually be called
      // so that we can reliably read any data-table-cell-colspan attribute.
      this.appendChild(dataTableCell);
      await dataTableCell.connectedPromise;

      const colspanEl = dataTableCell.querySelector(
        "[data-table-cell-colspan]"
      );
      if (colspanEl) {
        colSpanSkipRemaining =
          parseInt(colspanEl.getAttribute("data-table-cell-colspan"), 10) - 1;
      }

      colIdx += 1;
    }

    // Maybe disable selectability.
    if (selectionDisabled) {
      this.classList.add("unselectable");
      // Prevent click events from bubbling.
      this.clickHandler = (e) => e.stopPropagation();
      this.addEventListener("click", this.clickHandler);
    }

    // Invoke the connected promise resolver.
    this.connectedResolver();
  }

  disconnectedCallback() {
    this.removeEventListener("click", this.clickHandler);
  }

  set selected(selected) {
    /* Propagate selected value to selection checkbox checked state. */
    const { checkbox } = this.refs;
    if (checkbox) {
      checkbox.checked = selected;
    }
  }

  get selected() {
    /* Return the checkbox checked state. */
    const { checkbox } = this.refs;
    return checkbox.checked;
  }
}

customElementsMaybeDefine("data-table-row", DataTableRow, { extends: "tr" });
