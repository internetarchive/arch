import { populateTemplate } from "../lib/lib.js";

import { customElementsMaybeDefine, html } from "../lib/domLib.js";

export default class DataTableCell extends HTMLTableCellElement {
  constructor() {
    super();
    // Create a promise that will be resolved at the end of connectedCallback.
    this.connectedPromise = new Promise((resolve) => {
      this.connectedResolver = resolve;
    });
  }

  connectedCallback() {
    const {
      columnName,
      linkFormat,
      linkTarget,
      nullString,
      rowData,
      renderer,
    } = this.props;

    // Set the class.
    this.classList.add(columnName);

    // Get the discrete value for this column.
    let value = rowData;
    // If columnName was specified, split it to support dot-separated
    // property paths and retrieve the value.
    if (columnName) {
      for (const k of columnName.split(".")) {
        value = value[k];
      }
    }

    // Generate the HTML.
    let _html = "";
    if (!renderer) {
      // No rendered was specified.
      // If value is a supported scalar type, set html to the value.
      const valueType = typeof value;
      if (valueType === "string" || valueType === "number") {
        _html = `${value}`;
        // Set a title attribute for simple values in order to reveal
        // potentially truncated values.
        this.title = value;
      } else if (value === undefined || value === null) {
        _html = "";
      } else {
        // eslint-disable-next-line no-console
        console.warn(
          `Renderer not specified for value type (${valueType}) in column: ${columnName}`
        );
      }
    } else {
      // Apply the function or object/map-type renderer.
      const render =
        typeof renderer === "function"
          ? (v) => renderer(v, rowData) || ""
          : (v) => renderer[v] || "";
      // If value is an array, apply the renderer separately to each element and join.
      _html = Array.isArray(value)
        ? value.map((x) => render(x)).join("")
        : render(value);
    }

    // Maybe wrap html in a link.
    if (linkFormat && _html) {
      // If renderer was not used, add the "highlightable" class.
      _html = html`
        <a
          href="${populateTemplate(linkFormat, rowData)}"
          ${linkTarget !== "_self" ? `target="${linkTarget}"` : ""}
          ${!renderer ? 'class="highlightable"' : ""}
        >
          ${_html}
        </a>
      `;
    }

    this.innerHTML =
      _html ||
      html`<span class="null-placeholder highlightable">${nullString}</span>`;

    // Propagate any data-table-cell-colspan attribute specified by a child to this
    // element.
    const colspanEl = this.querySelector("[data-table-cell-colspan]");
    if (colspanEl) {
      this.colSpan = parseInt(
        colspanEl.getAttribute("data-table-cell-colspan"),
        10
      );
    }

    // Invoke the connected promise resolver.
    this.connectedResolver();
  }
}

customElementsMaybeDefine("data-table-cell", DataTableCell, { extends: "td" });
