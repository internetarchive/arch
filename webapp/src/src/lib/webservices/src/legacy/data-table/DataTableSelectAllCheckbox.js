import {
  customElementsMaybeDefine,
  html,
  removeChildren,
} from "../lib/domLib.js";

export default class DataTableSelectAllCheckbox extends HTMLElement {
  connectedCallback() {
    this.state = {
      numHits: 0,
      numSelected: 0,
    };

    this.innerHTML = html`
      <input type="checkbox" class="select-all" aria-label="Select All Rows" />
      <span class="fa fa-caret-down"></span>

      <ui5-popover placement-type="Bottom" horizontal-align="Left">
        <div class="popover-content">
          <ol></ol>
        </div>
      </ui5-popover>

      <ui5-popover
        placement-type="Top"
        horizontal-align="Left"
        class="loading"
        modal="true"
        hide-backdrop="true"
        style="--sapGroup_ContentBackground: #888;"
      >
        Selection Loading...
      </ui5-popover>
    `;

    const ol = this.querySelector("ol");
    const [optionPopover, loadingPopover] = this.querySelectorAll(
      ":scope > ui5-popover"
    );
    this.refs = {
      input: this.querySelector(":scope > input"),
      ol,
      popover: optionPopover,
      loading: loadingPopover,
    };
    this.updateOptions();

    this.addEventListener("click", this.clickHandler.bind(this));
    ol.addEventListener("click", this.itemClickHandler.bind(this));

    // Prevent loading popover clicks from triggering the option popover.
    loadingPopover.addEventListener("click", (e) => e.stopPropagation());
  }

  updateOptions() {
    const { numHits, numSelected } = this.state;
    const { ol } = this.refs;
    removeChildren(ol);
    if (!numHits && !numSelected) {
      ol.innerHTML = html`
        <li class="nothing-to-do">Nothing to select or clear</li>
      `;
      return;
    }
    if (numHits) {
      ol.innerHTML += html`
        <li data-action="SELECT_PAGE">Select All on this Page</li>
        <li data-action="SELECT_ALL">
          Select All <span class="num-hits">${numHits}</span> Items
        </li>
      `;
    }
    if (numSelected) {
      ol.innerHTML += html`
        <li data-action="SELECT_NONE">
          Clear Selection (<span class="num-selected">${numSelected}</span>)
        </li>
      `;
    }
  }

  set indeterminate(x) {
    /* this.refs.input.indeterminate proxy */
    const { input } = this.refs;
    input.indeterminate = x;
  }

  set checked(x) {
    /* this.refs.input.checked proxy */
    const { input } = this.refs;
    input.checked = x;
  }

  set numHits(x) {
    const { state } = this;
    state.numHits = x;
    this.updateOptions();
  }

  set numSelected(x) {
    const { state } = this;
    state.numSelected = x;
    this.updateOptions();
  }

  set loading(x) {
    const { loading } = this.refs;
    if (x) {
      loading.showAt(this);
    } else {
      loading.close();
    }
  }

  clickHandler(e) {
    /* Open or close the popover. */
    const { input, popover } = this.refs;
    e.preventDefault();
    e.stopPropagation();
    if (popover.open) {
      popover.close();
    } else {
      popover.showAt(input);
    }
  }

  itemClickHandler(e) {
    const { action } = e.target.dataset;
    if (action) {
      this.dispatchEvent(
        new CustomEvent("submit", {
          detail: { action },
          bubbles: true,
        })
      );
    }
    // Let the event bubble up to the clickHandle which will close the popover.
  }
}

customElementsMaybeDefine(
  "data-table-select-all-checkbox",
  DataTableSelectAllCheckbox
);
