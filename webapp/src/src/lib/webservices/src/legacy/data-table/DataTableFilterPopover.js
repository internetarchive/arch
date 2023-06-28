import "../vendor/ui5/UI5Popover.js";
import {
  customElementsMaybeDefine,
  html,
  parseElementProps,
} from "../lib/domLib.js";

export default class DataTableFilterPopover extends HTMLElement {
  connectedCallback() {
    this.props = this.props || {};

    this.props = Object.assign(
      parseElementProps(this, [
        ["API", this.props.API],
        ["column", this.props.column],
        ["distinctQueryApiPath", this.props.distinctQueryApiPath],
        ["header", this.props.header],
        ["initialSelectedValues", []],
        ["valueDisplayMap", {}],
      ]),
      this.props
    );

    const { header, initialSelectedValues } = this.props;

    this.state = {
      selectedValues: new Set(initialSelectedValues),
      options: undefined,
    };

    this.innerHTML = html`
      <button
        class="filter fa fa-cogs larger"
        aria-label="filter ${header} values"
      >
        <span class="applied-count" style="vertical-align: sub;"></span>
      </button>

      <ui5-popover placement-type="Bottom">
        <div class="popover-content">
          <button class="clear" style="display: none">
            <span class="fa fa-times-circle"></span>
            Clear Filters
          </button>
          <ol>
            <li><em style="color: #888;">Loading...</em></li>
          </ol>
        </div>
      </ui5-popover>
    `;

    this.filterButton = this.querySelector(":scope > button.filter");
    this.clearButton = this.querySelector(":scope button.clear");
    this.appliedCount = this.filterButton.querySelector(
      ":scope > span.applied-count"
    );
    this._popover = this.querySelector(":scope > ui5-popover");
    this.ol = this._popover.querySelector(":scope > div > ol");

    this.filterButton.addEventListener(
      "click",
      this.filterButtonClickHandler.bind(this)
    );
    this.clearButton.addEventListener(
      "click",
      this.clearButtonClickHandler.bind(this)
    );
    this._popover.addEventListener(
      "change",
      this.selectionChangeHandler.bind(this)
    );

    // If initialSelectedValues was specified, update the applied count.
    if (initialSelectedValues) {
      this.updateAppliedCount();
    }
  }

  optionToListItem(value) {
    const { valueDisplayMap } = this.props;
    const { selectedValues } = this.state;
    let label = value;
    // If value is an Array, parse it as [ value, label ].
    if (Array.isArray(value)) {
      [value, label] = value; // eslint-disable-line no-param-reassign
    }
    label = valueDisplayMap[label] || label;
    // Cast value to a string to check for membership in selectedValues since
    // selectedValues is populated by the input.name property which is always a string.
    const selected = selectedValues.has(`${value}`);
    return html`
      <li>
        <label>
          <input
            type="checkbox"
            name="${value}"
            style="margin-right: 1em;"
            ${selected ? "checked" : ""}
          />
          ${label}
        </label>
      </li>
    `;
  }

  renderOptions() {
    const { options } = this.state;
    this.ol.innerHTML = options.map(this.optionToListItem.bind(this)).join("");
  }

  updateOptions() {
    /* Issue the distinct query to the API and populate the option array with the
       results.
     */
    const { API, distinctQueryApiPath } = this.props;
    const { selectedValues } = this.state;
    API.get(distinctQueryApiPath).then(async (response) => {
      this.state.options = await response.json();
      this.renderOptions();
      this.cullOutdatedSelectionValues();
      // Show the Clear Filters button if any values are selected.
      this.clearButton.style.display =
        selectedValues.size > 0 ? "block" : "none";
    });
  }

  cullOutdatedSelectionValues() {
    /* If there are any selectedValues no longer in options, emit a deselect event
       for each, remove it from selectedValues, and update the applied count.
     */
    const { selectedValues } = this.state;
    const optionValues = new Set(
      Array.from(this.ol.querySelectorAll("input")).map((input) => input.name)
    );
    selectedValues.forEach((value) => {
      if (!optionValues.has(value)) {
        selectedValues.delete(value);
        this.emitSelectionEvent(value, false);
      }
    });
    this.updateAppliedCount();
  }

  static get observedAttributes() {
    /* Watch for asynchronous updates of valueDisplayMap.
     */
    return ["value-display-map"];
  }

  attributeChangedCallback(name, oldValue, newValue) {
    /* Handle asyncronous valueDisplayMap updates.
     */
    // Abort if this.props has not been initialized.
    if (!this.props) {
      return;
    }
    if (name === "value-display-map") {
      this.props.valueDisplayMap = JSON.parse(newValue);
      // If options has already been set, re-render.
      if (this.state.options) {
        this.renderOptions();
      }
    }
  }

  filterButtonClickHandler() {
    // Close the popover if it's open and return.
    if (this._popover.isOpen()) {
      this._popover.close();
      return;
    }
    // Kick off a distinct query if options haven't been loaded yet.
    const { options } = this.state;
    if (options === undefined) {
      this.updateOptions();
    }
    // Open the popover.
    this._popover.showAt(this.filterButton);
  }

  clearButtonClickHandler() {
    /* Remove any applied selections. */
    const { selectedValues } = this.state;
    this.emitDeselectAllEvent();
    selectedValues.clear();
    this.updateAppliedCount();
    this._popover.close();
  }

  updateAppliedCount() {
    const { selectedValues } = this.state;
    const { appliedCount } = this;
    const numApplied = selectedValues.size;
    if (numApplied === 0) {
      appliedCount.style.visibility = "hidden";
      appliedCount.textContent = "";
    } else {
      appliedCount.style.visibility = "visible";
      appliedCount.textContent = numApplied;
    }
  }

  emitSelectionEvent(value, selected) {
    const { column } = this.props;
    const event = `filter-${selected ? "select" : "deselect"}`;
    this.dispatchEvent(
      new CustomEvent(event, {
        detail: { column, value },
        bubbles: true,
      })
    );
  }

  emitDeselectAllEvent() {
    const { column } = this.props;
    this.dispatchEvent(
      new CustomEvent("filter-deselect-all", {
        detail: { column },
        bubbles: true,
      })
    );
  }

  selectionChangeHandler(e) {
    e.stopPropagation();
    const { target } = e;
    const { selectedValues } = this.state;
    selectedValues[target.checked ? "add" : "delete"](target.name);
    this.updateAppliedCount();
    this.emitSelectionEvent(target.name, target.checked);
    this._popover.close();
  }

  set selectedValues(values) {
    /* Setter for updating this.state.selectedValues and updating the UI. */
    const { options, selectedValues } = this.state;
    selectedValues.clear();
    values.forEach((value) => selectedValues.add(value));
    this.updateAppliedCount();
    // Re-render if options have already been initialized.
    if (options) {
      this.renderOptions();
    }
  }
}

customElementsMaybeDefine("data-table-filter-popover", DataTableFilterPopover);
