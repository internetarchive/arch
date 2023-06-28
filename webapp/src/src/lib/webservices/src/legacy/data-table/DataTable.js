/* eslint-disable no-param-reassign */

import "../vendor/ui5/UI5Popover.js";

import { formatChoices, populateTemplate, ThrottledLIFO } from "../lib/lib.js";

import {
  createElement,
  html,
  objToElementPropsStr,
  parseElementProps,
  removeChildren,
  slugify,
} from "../lib/domLib.js";

import AngularMixin from "../mixins/AngularMixin.js";

import "./Paginator.js";
import DataTableFilterPopover from "./data-table-filter-popover.js";
import DataTableRow from "./DataTableRow.js";
import DataTableSelectAllCheckbox from "./DataTableSelectAllCheckbox.js";

export default class DataTable extends AngularMixin(HTMLElement) {
  connectedCallback() {
    // Init props if not already set at pre-connect.
    this.props = this.props || {};
    this.setReadyHandler(DataTable.observedAttributes, this.doConnect);
  }

  async doConnect() {
    /* Run only after all attributes values have been interpolated.
     */
    this.throttledSearchLIFO = ThrottledLIFO();

    // Parse supported attribute values from the element, overriding with any
    // pre-connect specified props.
    this.props = Object.assign(
      parseElementProps(this, [
        /* actionButtonClasses is an array of string to append to the class
           attribute of the corresponding action button <button> element.
         */
        ["actionButtonClasses", []],

        /* actionButtonLabels is an array of string labels to display
           as the textContent of the action buttons.
         */
        ["actionButtonLabels", []],

        /* actionButtonDisabled is an array of bools indicating whether the
           action button should always be disabled.
         */
        ["actionButtonDisabled", []],

        /* actionButtonSignals is an array of signal name strings that indicate
           the signal to emit when the button is clicked.
         */
        ["actionButtonSignals", []],

        /* API is an optional custom API class that provides a get() method that
           this component will use to fetch data.
         */
        ["API", this.props.API],

        /* API_BASE_PATH is the base path to access the API, not including the
           trailing slash, e.g. "/api"
         */
        ["API_BASE_URL", this.props.apiBaseUrl],

        /* apiCollectionEndpoint is an API-relative URL path that corresponds to the
           collection endpoint, e.g. "/seed".
         */
        ["apiCollectionEndpoint", this.props.apiCollectionEndpoint],

        /* apiItemResponseIsArray is a bool indicate whether the API response from
           apiItemTemplate will be a single-element Array.
         */
        ["apiItemResponseIsArray", false],

        /* apiItemTemplate is an API-relative URL path template that, when
           iterpolated with an item object property(s), will retrieve that item,
           e.g. "/seed/:id"
         */
        ["apiItemTemplate", null],

        /* apiStaticParamPairs is an array of two-element [ <field>, <value> ]
           arrays defining the base query params, e.g. [[ 'collection_id', 1 ]]
         */
        ["apiStaticParamPairs", []],

        /* cellRenderers is an array of objects or functions positionally
           mapped to columns that can be used to convert the column value into
           custom HTML. For example:
             To convert values in the first column to an eyes-friendly format:

               [ { "DAILY": "Daily", "MONTHLY": "Monthly" } ]

             To wrap all values in the first column in an <em>:

               [ value => `<em>${value}</em>` ]
         */
        ["cellRenderers", []],

        /* rowIdColumn is a string that indicates which API response
           object field will be used to set the data-id attribute on each <tr>.
         */
        ["rowIdColumn", "id"],

        /* columnHeaders is an array of column display name strings positionally
           mapped to columns.
         */
        ["columnHeaders", []],

        /* columns is an array of API response object field name strings to use
           in building the content for each table column.
         */
        ["columns", this.props.columns],

        /* columnSortParamMap is a columnName -> sortParamName map that optionally
           indicates the sort param value to use instead of the column name itself.
         */
        ["columnSortParamMap", {}],

        /* filterableColumns is an array of bools positionally mapped to
           columns to indicate whether the column is filterable.
         */
        ["filterableColumns", []],

        /* columnFilterDisplayMaps is an array of objects positionally mapped to
           columns indicating columnValue -> displayValue mappings to use when
           displaying options in the column filter popovers, e.g.

             To display eye-friendly names instead of integer IDs:

               [ { 1: "Group 1", 2: "Group 2" } ]
         */
        ["columnFilterDisplayMaps", []],

        /* columnFilterParams is an array of objects defining the custom query params
           used to retrieve a column's filter values. If no value is specified for a
           column, the default of { distinct: columnName } will be used.

             For example, to retrieve a string-type status value in column 0:

               [ { distinct: "status" } ]

             To retrieve a numeric ID, and string-type name field to use as the label:

               [ { distinct: "id,name" } ]

             If elements in the response array are themselves arrays, the first element
             will be used as the filter value and the second as the label value.
         */
        ["columnFilterParams", []],

        /* csvDownloadHeaderColumnPairs is an array of
           [<csv-header-name>, <instance-column-name>] pairs that will be used to
           specify the downloaded CSV format.
         */
        ["csvDownloadHeaderColumnPairs", []],

        /* csvDownloadExtraQueryParams is an object specifying additional query params
           to specify in the CSV download link URL. This is useful is you need to
           annotate__concat some field in order to transform it into a comma-separated
           list that the drf-csv library can handle.
         */
        ["csvDownloadExtraQueryParams", {}],

        /* focusSearch is a bool indicating whether to automatically focus the
           search input on component load.
         */
        ["focusSearch", false],

        /* highlightMatches is a bool that indicates whether to highlight
           matching text in the columns mapped by searchColumns.

           Note that you must add the "highlightable" class to the element that
           wraps the text in order for highlighting to work.
         */
        ["highlightMatches", true],

        /* itemPollPredicate is a function that accepts a row object and returns
           a bool indicate whether we should poll for updates.
         */
        ["itemPollPredicate", null],

        /* itemPollPeriodSeconds is the number of seconds we should wait between
           poll requests for rows that satisfy the itemPollPredicate function.
         */
        ["itemPollPeriodSeconds", 3],

        /* persistSearchStateInUrl is a bool indicating whether to read the initial
           search state from the URL params and use the browser history API to
           store/sync any search state changes to URL params.
         */
        ["persistSearchStateInUrl", false],

        /* linkFormats is an array of URL format strings positionally mapped to
           columns that will result in the the value being wrapped in an <a> tag
           with the specified, interpolated href value.
           See lib.populateTemplate() for details on the supported format.
         */
        ["linkFormats", []],

        /* linkTargets is an array of <a> "target" property strings
           positionally mapped to linkFormats.
         */
        ["linkTargets", []],

        /* loadingMessage is HTML string to display during the API fetch.
         */
        ["loadingMessage", "Loading..."],

        /* noInitialSearch is a bool indicating whether to refrain from initiating a
           search on doConnect().
         */
        ["noInitialSearch", false],

        /* nonSelectionActionLabels is an array of string labels to display
           as the textContent of the non-selection-related action buttons.

           TODO: rename this to "nonSelectionActionButtonLabels" for
           consistency with "actionButtonLables".
         */
        ["nonSelectionActionLabels", []],

        /* nonSelectionActions is an array of signal name strings positionally
           mapped to nonSelectionActionLabels that indicates the signal to
           emit when the button is clicked.

           TODO: rename this to "nonSelectionActionButtonSignals" for
           consistency with "actionButtonSignals".
         */
        ["nonSelectionActions", []],

        /* noResultsText is the string to display when a query returns 0 results.
         */
        ["noResultsText", "No Results"],

        /* nullString is an HTML string to use in place of a column field value.
         */
        ["nullString", "&mdash;"],

        /* pageLength is the default max rows page size.
         */
        ["pageLength", 100],

        /* pluralName is a string indicating the plural name of the table
           collection, e.g. "Seeds"
         */
        ["pluralName", this.props.pluralName],

        /* rowClickEnabled is a boolean indicating whether a click on a table
           row should toggle the corresponding selection checkbox.
         */
        ["rowClickEnabled", true],

        /* rowSelectDisabledCallback is an optional function that will be invoked
           with each row object and should return a bool indicate whether
           selectability should be disabled for the row.
         */
        ["rowSelectDisabledCallback", () => false],

        /* searchColumns is an array of field specifier strings that will be
           passed via the search_fields param during a search. This array is
           positionally mapped to the columns prop for the purpose of
           highlighting text matches within those columns.
         */
        ["searchColumns", this.props.searchColumns],

        /* searchColumnLables is an array of strings that's positionally mapped
           to searchColumns to indicate the display labels for searchable fields,
           e.g. for inclusion in the
           "Type to filter {model} by {searchColumnLables}" input element placeholder
           value.
         */
        ["searchColumnLabels", this.props.searchColumnLabels],

        /* selectAllExtraQueryParams is an object specifying any extra query params
           that should be applied for select-all operations.
         */
        ["selectAllExtraQueryParams", null],

        /* selectable is a bool that indicates whether to display row checkboxes
           and selection-related action buttons.
         */
        ["selectable", true],

        /* showDownloadLink is a bool indicating whether to show the
           "Download {pluralName} List" link.
         */
        ["showDownloadLink", false],

        /* singleName is a string indicating the singular name of the table
           collection, e.g. "Seed"
         */
        ["singleName", this.props.singleName],

        /* sort is a string indicating the initial, single-column Django
           queryset.order_by() value.
         */
        ["sort", "-id"],

        /* sortableColumns is an array of bools positionally mapped to columns
           indicating whether sorting is enable for that column.
         */
        ["sortableColumns", []],
      ]),
      this.props
    );

    const {
      API,
      actionButtonClasses,
      actionButtonLabels,
      actionButtonSignals,
      columnFilterDisplayMaps,
      columnFilterParams,
      columnHeaders,
      columns,
      csvDownloadHeaderColumnPairs,
      filterableColumns,
      focusSearch,
      persistSearchStateInUrl,
      noInitialSearch,
      nonSelectionActionLabels,
      nonSelectionActions,
      pageLength,
      pluralName,
      searchColumns,
      searchColumnLabels,
      selectable,
      showDownloadLink,
      singleName,
      sort,
      sortableColumns,
    } = this.props;

    this.state = {
      rows: [],
      rowsToPoll: [],
      rowElements: [],
      selectedIdRowMap: new Map(),
      lastRowClickIndex: undefined,
      search: {
        limit: pageLength,
        numHits: 0,
        pageNum: 1,
        q: "",
        sort,
        // filters is a column => Set([ columnValue, ... ]) object populated
        // by this.filterSelectionHandler().
        filters: {},
      },
    };

    const { search: searchState } = this.state;

    // Initialize element references for convenient access.
    this.refs = {
      selectAllCheckbox: null,
      filterPopovers: [],
    };

    // Remove any existing children in the event of a reconnect.
    removeChildren(this);

    if (persistSearchStateInUrl) {
      // Update this.state.search from the current URL params.
      this.setSearchStateFromCurrentUrlParams();
    }

    // Append tha Paginator wrapper element.
    this.paginatorWrapperEl = createElement(
      '<div class="paginator-wrapper"></div>'
    );
    this.appendChild(this.paginatorWrapperEl);
    // Register the paginator button click handler.
    this.paginatorWrapperEl.addEventListener(
      "click",
      this.paginatorClickHandler.bind(this)
    );

    // Add the search input elements if searchColumns is defined.
    if (Array.isArray(searchColumns) && searchColumns.length) {
      // Append the search input and attach the input handler.
      const choiceListStr = formatChoices(searchColumnLabels.filter(Boolean));
      const searchableFieldsStr = choiceListStr.length
        ? `by ${choiceListStr}`
        : "";
      const searchInputEl = createElement(html`
        <input
          type="text"
          placeholder="Type to filter ${pluralName} ${searchableFieldsStr}"
          class="search"
        />
      `);
      // Set the search input value to the initial q value.
      searchInputEl.value = searchState.q;
      searchInputEl.addEventListener(
        "input",
        this.searchInputHandler.bind(this)
      );
      this.appendChild(searchInputEl);
      // Maybe focus the search input.
      if (focusSearch) {
        searchInputEl.focus();
      }

      // Append a search input clear button.
      this.searchInputClearEl = createElement(html`
        <button class="clear-input">&times;</button>
      `);
      this.searchInputClearEl.style.visibility = searchState.q
        ? "visible"
        : "hidden";
      // Clear the search input and dispatch an "input" event on click.
      this.searchInputClearEl.addEventListener("click", () => {
        searchInputEl.value = "";
        searchInputEl.dispatchEvent(new Event("input"));
      });
      this.appendChild(this.searchInputClearEl);
    }

    // Maybe add the download link.
    if (showDownloadLink && csvDownloadHeaderColumnPairs.length) {
      this.downloadLink = createElement(html`
        <a
          href="#"
          class="download"
          target="_blank"
          download="${singleName.toLowerCase()}-list.csv"
        >
          <span class="fa fa-download"></span>
          Download ${singleName} List
        </a>
      `);
      // Set the initial href.
      this.updateDownloadLinkHref();
      this.appendChild(this.downloadLink);
    }

    // Add the action buttons.
    const actionButtonWrapperEl = createElement(html`
      <div class="action-button-wrapper">
        <div class="selection-buttons">
          ${!selectable
            ? ""
            : actionButtonLabels
                .map(
                  (label, i) => html`
                    <button
                      name="${label}"
                      class="selection-action button ${actionButtonClasses[i] ||
                      ""}"
                      data-signal="${actionButtonSignals[i]}"
                      disabled
                    >
                      ${label}
                    </button>
                  `
                )
                .join("\n")}
        </div>
        <div class="non-selection-buttons">
          ${nonSelectionActions
            .filter((action) => action !== undefined)
            .map((action, i) => {
              const label = nonSelectionActionLabels[i];
              return html`
                <button name="${label}" data-signal="${action}">
                  ${label}
                </button>
              `;
            })
            .join("\n")}
        </div>
      </div>
    `);
    this.appendChild(actionButtonWrapperEl);
    // Register the action button click handler.
    actionButtonWrapperEl.addEventListener(
      "click",
      this.actionButtonClickHandler.bind(this)
    );

    // Create the main table.
    const table = document.createElement("table");
    this.appendChild(table);

    // Maybe add the table header.
    if (columnHeaders.length > 0) {
      this.thead = document.createElement("thead");
      table.appendChild(this.thead);
      const tr = document.createElement("tr");
      this.thead.appendChild(tr);
      // Add a select-all checkbox cell if selectable.
      if (selectable) {
        this.refs.selectAllCheckbox = new DataTableSelectAllCheckbox();
        this.refs.selectAllCheckbox.addEventListener(
          "submit",
          this.selectAllCheckboxSubmitHandler.bind(this)
        );
        const th = createElement(html`<th class="select"></th>`, "tr");
        th.appendChild(this.refs.selectAllCheckbox);
        tr.appendChild(th);
      }

      columnHeaders.forEach((header, i) => {
        const th = createElement(
          html` <th class="${slugify(header)}"></th> `,
          "tr"
        );
        const columnName = columns[i];

        if (!sortableColumns[i]) {
          th.innerHTML += header;
        } else {
          const sortButton = createElement(html`
            <button
              data-i="${i}"
              class="sort"
              aria-label="sort by ${header} ascending"
            >
              ${header}
            </button>
          `);
          // Visually indicate any initial active sort.
          const sortDesc = searchState.sort.startsWith("-");
          if (searchState.sort === `${sortDesc ? "-" : ""}${columnName}`) {
            sortButton.classList.add(sortDesc ? "sort-desc" : "sort-asc");
          }
          th.appendChild(sortButton);
        }

        if (filterableColumns[i]) {
          // Add the column name as a class value to enable querySelection.
          const filterPopover = new DataTableFilterPopover();
          filterPopover.classList.add(columnName);
          filterPopover.props = {
            API,
            distinctQueryApiPath: this.getHitsOrDistinctQueryApiPath(
              columnFilterParams[i] || { distinct: columnName },
              true
            ),
            column: columnName,
            header,
            valueDisplayMap: columnFilterDisplayMaps[i] || {},
            initialSelectedValues: searchState.filters[columnName] || [],
          };
          this.refs.filterPopovers.push(filterPopover);
          th.appendChild(filterPopover);
        }
        tr.appendChild(th);
      });

      // Register the header click handler.
      this.thead.addEventListener("click", this.theadClickHandler.bind(this));
      // Register the header filter select/deselect handlers.
      for (const event of [
        "filter-select",
        "filter-deselect",
        "filter-deselect-all",
      ]) {
        this.thead.addEventListener(
          event,
          this.filterSelectionHandler.bind(this)
        );
      }
    }

    // Create the table body.
    this.tbody = document.createElement("tbody");
    table.appendChild(this.tbody);

    // Maybe register the row click handler.
    if (selectable) {
      this.tbody.addEventListener("click", this.rowClickHandler.bind(this));
    }

    // Register the delete-row handler.
    this.tbody.addEventListener("delete-row", this.deleteRowHandler.bind(this));

    // Maybe do an initial search. Do not reset page num.
    if (!noInitialSearch) {
      this.throttledDoSearch(false);
    }
  }

  static get observedAttributes() {
    /* Return the names of properties to which changes will trigger
       attributeChangedCallback().
     */
    return [
      "action-button-classes",
      "action-button-labels",
      "action-button-disabled",
      "action-button-signals",
      "api-collection-endpoint",
      "api-static-param-pairs",
      "cell-templates",
      "cell-renderers",
      "checkbox-name-column",
      "column-filter-display-maps",
      "column-headers",
      "columns",
      "filterable-columns",
      "focus-search",
      "highlight-matches",
      "persist-search-state-in-url",
      "link-formats",
      "link-targets",
      "no-initial-search",
      "non-selection-action-labels",
      "non-selection-actions",
      "null-string",
      "page-length",
      "plural-name",
      "search-columns",
      "search-column-labels",
      "selectable",
      "single-name",
      "sort",
      "sortable-columns",
    ];
  }

  setSearchStateFromCurrentUrlParams() {
    /* Update this.state.search from any current URL params. */
    const { columns, filterableColumns, sort } = this.props;
    const { search: searchState } = this.state;
    const params = new URLSearchParams(window.location.search);

    // Update q
    searchState.q = params.has("q") ? decodeURIComponent(params.get("q")) : "";

    // Update sort
    searchState.sort = params.has("sort")
      ? decodeURIComponent(params.get("sort"))
      : sort;

    // Update pageNum
    searchState.pageNum = parseInt(
      params.has("page") ? decodeURIComponent(params.get("page")) : 1,
      10
    );

    // Update filters.
    const filterableColumnNames = columns.filter(
      (column, i) => filterableColumns[i]
    );
    const filterKeyPrefix = "column-";
    const filterKeyPrefixLength = filterKeyPrefix.length;
    Array.from(params.entries()).forEach(([k, v]) => {
      // Ignore params without the expected prefix and/or suffix.
      if (!k.startsWith(filterKeyPrefix)) {
        return;
      }
      const column = k.slice(filterKeyPrefixLength);
      // Ignore invalid column names.
      if (!filterableColumnNames.includes(column)) {
        return;
      }
      // Looks good. Split the value on commas and update filters.
      searchState.filters[column] = new Set(decodeURIComponent(v).split(","));
    });
  }

  persistSearchStateInUrl() {
    /* Use the history API to update the URL params to match the
       current filters set. */
    const { search: searchState } = this.state;
    const { pageNum, q, sort, filters } = searchState;
    const url = new URL(window.location.href);
    // Remove any existing specified q, sort, and column filter params.
    const { searchParams } = url;
    searchParams.delete("q");
    searchParams.delete("sort");
    searchParams.delete("page");
    Array.from(searchParams.keys())
      .filter((k) => k.startsWith("column-"))
      .forEach((k) => searchParams.delete(k));
    // Set the new params from the filters.
    if (q) {
      searchParams.set("q", encodeURIComponent(q));
    }
    if (sort) {
      searchParams.set("sort", encodeURIComponent(sort));
    }
    // Always set page.
    searchParams.set("page", pageNum);
    for (const [columnName, values] of Object.entries(filters)) {
      // Apparently need to explictly encode the comma-separated list in order for
      // the history functions to work as expected.
      searchParams.set(
        `column-${columnName}`,
        encodeURIComponent(Array.from(values).join(","))
      );
    }

    // eslint-disable-next-line no-restricted-globals
    history.replaceState(null, "", url);
  }

  attributeChangedCallback(name, oldValue, newValue) {
    if (super.attributeChangedCallback(name, oldValue, newValue)) {
      // Event was handled.
      return;
    }

    switch (name) {
      case "api-static-param-pairs":
        // Update this.props and refresh the rows.
        this.props.apiStaticParamPairs = JSON.parse(newValue);
        this.doSearch();
        break;
      default:
        // eslint-disable-next-line no-console
        console.warn(
          `Unhandled "${name}" attribute change: ${oldValue} -> ${newValue}`
        );
    }
  }

  getRowId(row) {
    /* Return the row's corresponding rowIdColumn property value as a string. */
    const { rowIdColumn } = this.props;
    return `${row[rowIdColumn]}`;
  }

  enrichRowObject(row) {
    /* Enrich a row object with DataTable-specific properties. */
    // Add a _dataTableRowId property.
    row._dataTableRowId = this.getRowId(row);
  }

  // eslint-disable-next-line class-methods-use-this
  unenrichRowObject(row) {
    /* Return the row object with DataTable-specific properties removed. */
    delete row._dataTableRowId;
    return row;
  }

  updatePaginator() {
    const { singleName, pluralName } = this.props;
    const { limit, numHits, pageNum } = this.state.search;

    const start = (pageNum - 1) * limit + 1;
    this.paginatorWrapperEl.innerHTML = html`
      <span class="showing">
        ${singleName} List (${numHits ? start : 0} to
        ${Math.min(numHits, start + limit - 1)} of ${numHits} ${pluralName})
      </span>

      <paginator-control
        ${objToElementPropsStr({
          numTotal: numHits,
          pageSize: limit,
          currentPage: pageNum,
        })}
      >
      </paginator-control>
    `;
  }

  shouldShortCiruitSearch() {
    /* Return a bool indicating whether we should short-circuit this search.
     */
    for (const [k, v] of this.props.apiStaticParamPairs) {
      // Short-circuit if __in argument is empty.
      if (k.endsWith("__in") && !v.length) {
        return true;
      }
    }
    return false;
  }

  buildAPIPath(params) {
    return `${this.props.apiCollectionEndpoint}?${params.toString()}`;
  }

  buildRequestParams(excludeFilters = false) {
    /* Return a URLSearchParams object based on the current values of
       this.props.apiStaticParamPairs and this.state.search.
     */
    const { limit, filters, pageNum, q, sort } = this.state.search;
    const { apiStaticParamPairs, searchColumns } = this.props;
    // Init a URLSearchParams object using apiStaticParamPairs.
    const params = new URLSearchParams(apiStaticParamPairs);
    // Apply limit and offset.
    params.set("limit", limit);
    params.set("offset", limit * (pageNum - 1));
    // Maybe add the filter {column}__in params.
    if (!excludeFilters) {
      for (const [column, valueSet] of Object.entries(filters)) {
        params.set(`${column}__in`, Array.from(valueSet.values()));
      }
    }
    // Apply search.
    if (q) {
      params.set("search", q);
      params.set("search_fields", searchColumns);
    }
    // Apply sort.
    if (sort) {
      params.set("sort", sort);
    }
    return params;
  }

  getHitsOrDistinctQueryApiPath(extraParams = {}, excludeFilters = false) {
    /* Return the API path for a stripped-down version of the current search
       params that omits annotate__* and sets limit=-1 that's appropriate for total
       hits and filter distinct queries.
     */
    const params = this.buildRequestParams(excludeFilters);
    Object.entries(extraParams).forEach(([k, v]) => params.append(k, v));
    Array.from(params.keys())
      .filter((k) => k.startsWith("annotate__"))
      .forEach((k) => params.delete(k));
    params.set("limit", "-1");
    params.delete("sort");
    params.delete("offset");
    return this.buildAPIPath(params);
  }

  async doTotalHitsQuery() {
    // Handle a short-circuit condition.
    if (this.shouldShortCiruitSearch()) {
      this.state.search.numHits = 0;
      this.updatePaginator();
      return;
    }
    const { API, selectable, selectAllExtraQueryParams } = this.props;
    const { search } = this.state;
    const { selectAllCheckbox } = this.refs;

    const getNumHits = async (extraParams = {}) => {
      const apiPath = this.getHitsOrDistinctQueryApiPath({
        __count: "id",
        ...extraParams,
      });
      const response = await API.get(apiPath);
      const data = await response.json();
      const { id__count: numHits } = data;
      return numHits;
    };

    // Update the search state.
    search.numHits = await getNumHits();

    // Update the select-all checkbox.
    if (selectable) {
      if (!selectAllExtraQueryParams) {
        selectAllCheckbox.numHits = search.numHits;
      } else {
        selectAllCheckbox.numHits = await getNumHits(selectAllExtraQueryParams);
      }
    }

    // Update the paginator.
    this.updatePaginator();
  }

  async doSearch(resetPageNum = true) {
    const { API, itemPollPredicate, persistSearchStateInUrl } = this.props;

    // Reset pageNum by default on new searches, but allow disabling the reset,
    // e.g. for pagination.
    if (resetPageNum) {
      this.state.search.pageNum = 1;
    }

    // Maybe persist the search state in the URL.
    if (persistSearchStateInUrl) {
      this.persistSearchStateInUrl();
    }

    // Handle a short-circuit condition.
    if (this.shouldShortCiruitSearch()) {
      this.state.rowsToPoll = [];
      this.state.rows = [];
      this.showNoResultsRow();
      return;
    }
    // Kick off a count query.
    this.doTotalHitsQuery();
    const response = await API.get(
      this.buildAPIPath(this.buildRequestParams())
    );
    const rows = await response.json();
    // Check for an error response.
    if (!Array.isArray(rows) && rows.error) {
      throw new Error(rows.error);
    }
    if (rows.length > 0) {
      this.state.rows = rows;
      // Add a _dataTableRowId property to each row.
      rows.forEach((row) => this.enrichRowObject(row));
      this.updateRows();
      // Set this.state.rowsToPoll to the array of rows that satisfy any
      // specified itemPollPredicate.
      if (itemPollPredicate) {
        this.setRowsToPoll(rows.filter(itemPollPredicate));
      }
    } else {
      this.showNoResultsRow();
    }
    // Maybe update the download link href.
    if (this.downloadLink) {
      this.updateDownloadLinkHref();
    }
  }

  setRowsToPoll(rows) {
    /* Set state.rowsToPoll and kick off polling if non-empty */
    const { itemPollPeriodSeconds } = this.props;
    this.state.rowsToPoll = rows;
    if (rows.length > 0) {
      setTimeout(this.pollRows.bind(this), itemPollPeriodSeconds * 1000);
    }
  }

  async pollRows() {
    /* Fetch fresh copies of the rows in this.state.rowsToPoll from the API. */
    const {
      API,
      apiItemResponseIsArray,
      apiItemTemplate,
      itemPollPeriodSeconds,
      itemPollPredicate,
      rowIdColumn,
    } = this.props;
    const { rows, rowsToPoll } = this.state;
    // Abort if rowsToPoll has been cleared.
    if (rowsToPoll.length === 0) {
      return;
    }
    // Request rows from the API.
    let polledRows = await Promise.all(
      rowsToPoll.map(async (row) =>
        (await API.get(populateTemplate(apiItemTemplate, row))).json()
      )
    );
    // Unpack single-element Array-type responses.
    if (apiItemResponseIsArray) {
      polledRows = polledRows.map((row) => row[0]);
    }
    // Update rowsToPoll with the rows that still require polling.
    this.state.rowsToPoll = polledRows.filter(itemPollPredicate);
    // Schedule the next poll if any pollable rows remain.
    if (this.state.rowsToPoll.length > 0) {
      setTimeout(this.pollRows.bind(this), itemPollPeriodSeconds * 1000);
    }
    // If no polledRows satisfies the predicate this cycle, abort.
    if (polledRows.length === this.state.rowsToPoll.length) {
      return;
    }
    // Update this.stats.rows with polledRows.
    const polledRowIdRowMap = new Map(
      polledRows.map((row) => [row[rowIdColumn], row])
    );
    for (let i = 0; i < rows.length; i += 1) {
      const rowId = rows[i][rowIdColumn];
      if (polledRowIdRowMap.has(rowId)) {
        rows[i] = polledRowIdRowMap.get(rowId);
      }
    }
    // Update the UI.
    this.updateRows();
  }

  async throttledDoSearch(resetPageNum = true) {
    this.showLoadingRow();
    try {
      await this.throttledSearchLIFO(async () => this.doSearch(resetPageNum));
    } catch (e) {
      if (e !== "throttled") {
        throw e;
      }
    }
  }

  showLoadingRow() {
    const { loadingMessage } = this.props;
    removeChildren(this.tbody);
    this.tbody.appendChild(
      createElement(
        html`
          <tr>
            <td
              colspan="${this.props.columns.length +
              (this.props.selectable ? 1 : 0)}"
              class="loading"
            >
              ${loadingMessage}
            </td>
          </tr>
        `,
        "tbody"
      )
    );
  }

  clearSelections() {
    /* Clear any active row selections. */
    const { selectable } = this.props;
    const { rowElements, selectedIdRowMap } = this.state;
    if (!selectable) {
      return;
    }
    selectedIdRowMap.clear();
    this.postSelectionChangeHandler();
    // Uncheck all selection checkboxes on the current page.
    rowElements.forEach((tr) => {
      tr.selected = false;
    });
    // Disable all selection action buttons.
    this.setSelectionActionButtonDisabledState(true);
  }

  showNoResultsRow() {
    removeChildren(this.tbody);
    const { noResultsText } = this.props;
    this.tbody.appendChild(
      createElement(
        html`
          <tr>
            <td
              colspan="${this.props.columns.length +
              (this.props.selectable ? 1 : 0)}"
              class="no-results"
            >
              ${noResultsText}
            </td>
          </tr>
        `,
        "tbody"
      )
    );
  }

  async updateSelectedRows() {
    /* Re-fetch the selected rows from the server to ensure that the row objects in
       selectedIdRowMap reflect the current state of things. */
    const { API, apiStaticParamPairs, rowIdColumn } = this.props;
    const { selectedIdRowMap } = this.state;
    // Fetch the updated rows from the API.
    const params = new URLSearchParams(apiStaticParamPairs);
    params.set("limit", "-1");
    params.set(`${rowIdColumn}__in`, Array.from(selectedIdRowMap.keys()));
    const apiPath = this.buildAPIPath(params);
    const response = await API.get(apiPath);
    const rows = await response.json();
    // Update the selectedIdRowMap row objects.
    for (const row of rows) {
      this.enrichRowObject(row);
      selectedIdRowMap.set(row._dataTableRowId, row);
    }
  }

  async updateRows() {
    const {
      cellRenderers,
      columns,
      highlightMatches,
      linkFormats,
      linkTargets,
      nullString,
      rowClickEnabled,
      rowSelectDisabledCallback,
      selectable,
    } = this.props;

    const { selectedIdRowMap, rows, rowElements, search } = this.state;

    // truncate the rowElements array.
    rowElements.length = 0;

    // Remove any existing rows from the tbody.
    removeChildren(this.tbody);

    // Append the new rows.
    let rowIdx = 0;
    for await (const row of rows) {
      const dataTableRow = new DataTableRow();
      // Determine selected status based on whether the stringified (because
      // subsequently reading input.name yields a string) checkbox name value is
      // set in selectedIdRowMap.
      const { _dataTableRowId: rowId } = row;
      const selected = selectedIdRowMap.has(rowId);
      if (selected) {
        // Update selectedIdRowMap with the current row object to ensure that the
        // selection objects are up-to-date.
        selectedIdRowMap.set(rowId, row);
      }
      dataTableRow.props = {
        cellRenderers,
        columns,
        data: row,
        linkFormats,
        linkTargets,
        nullString,
        selectable,
        selectionDisabled: rowSelectDisabledCallback(row),
        selected,
      };
      // Set the data-i property.
      dataTableRow.dataset.i = rowIdx;
      // Set the data-id property.
      dataTableRow.dataset.id = rowId;
      // Maybe add the "clickable" class.
      if (selectable && rowClickEnabled) {
        dataTableRow.classList.add("clickable");
      }
      // Append the row to rowElements.
      rowElements.push(dataTableRow);
      // Append the row and wait for it to actually be connected.
      this.tbody.appendChild(dataTableRow);
      await dataTableRow.connectedPromise;
      rowIdx += 1;
    }
    // Maybe highlight the query matches substrings.
    if (search.q && highlightMatches) {
      this.highlightMatches();
    }
    // Update any initialized filters.
    this.updateInitializedFilterPopovers();

    // Announce the update.
    this.emit("ROWS_UPDATED");
  }

  clear() {
    /* Reset the table and clear the rows. */
    this.state.rowsToPoll = [];
    this.state.rows = [];
    this.updateRows();
  }

  updateInitializedFilterPopovers() {
    /* For each column filter popover that has already initialized its state.options,
       invoke updateOptions() to refresh the options.
     */
    const { filterPopovers } = this.refs;
    filterPopovers.forEach((filterPopover) => {
      const { options } = filterPopover.state;
      if (options !== undefined) {
        filterPopover.updateOptions();
      }
    });
  }

  async searchInputHandler(e) {
    const { value } = e.target;
    // Set the visibility of the input clear button based on whether the input is empty.
    this.searchInputClearEl.style.visibility = value ? "visible" : "hidden";
    this.state.search.q = value;
    this.throttledDoSearch();
  }

  highlightMatches() {
    // Escape the query string as a regexp: https://stackoverflow.com/a/3561711/2327940
    const regex = new RegExp(
      // eslint-disable-next-line no-useless-escape
      this.state.search.q.replace(/[-\/\\^$*+?.()|[\]{}]/g, "\\$&"),
      "gi"
    );
    const replacer = (match) =>
      html`<span class="match-highlight">${match}</span>`;

    // Format the compound CSS selector strings to retrieve all
    // searchable/highlightable <td> elements.
    const selectorStr = this.props.searchColumns
      .map((columnName) => `td.${columnName}`)
      .join(", ");

    Array.from(this.tbody.querySelectorAll(selectorStr)).forEach((td) => {
      // Check for an element with the "highlightable" class.
      let els = Array.from(td.querySelectorAll(".highlightable"));
      if (els.length === 0) {
        // No highlightable elements found, so target the <td> itself.
        els = [td];
      }
      // Add the highlights.
      els.forEach((el) => {
        el.innerHTML = el.textContent.replace(regex, replacer);
      });
    });
  }

  paginatorClickHandler(e) {
    const { target } = e;
    if (target.tagName !== "BUTTON") {
      return;
    }
    this.state.search.pageNum = parseInt(target.getAttribute("page"), 10);
    this.doSearch(false);
  }

  actionButtonClickHandler(e) {
    /* Emit the corresponding signal on action button click.
     */
    const { target } = e;
    if (target.tagName !== "BUTTON") {
      return;
    }
    const items = !target.classList.contains("selection-action")
      ? null
      : Array.from(this.state.selectedIdRowMap.values());
    this.emit(target.dataset.signal, items);
  }

  setSelectionActionButtonDisabledState(disabled) {
    /* Set the selection actions buttons to the specified disabled state
       unless the button is permanently disabled in props.actionButtonDisabled.
     */
    this.querySelectorAll("button.selection-action").forEach((el, i) => {
      el.disabled = this.props.actionButtonDisabled[i] || disabled;
    });
  }

  postSelectionChangeHandler() {
    /* Update the action button and select-all checkbox states and emit a
       SELECTION_CHANGE signal. */
    const { selectedIdRowMap } = this.state;
    this.setSelectionActionButtonDisabledState(selectedIdRowMap.size === 0);
    this.updateSelectAllCheckboxState();
    this.emit("SELECTION_CHANGE", {
      selectedRows: Array.from(selectedIdRowMap.values()),
    });
  }

  updateSelectAllCheckboxState() {
    // Update the select all checkbox to reflect the current row selection state.
    const { selectedIdRowMap } = this.state;
    const { numHits } = this.state.search;
    const { selectAllCheckbox } = this.refs;
    const { size: numSelected } = selectedIdRowMap;
    selectAllCheckbox.numSelected = numSelected;
    if (numSelected > 0 && numSelected < numHits) {
      selectAllCheckbox.indeterminate = true;
    } else {
      selectAllCheckbox.indeterminate = false;
      selectAllCheckbox.checked = numSelected >= numHits;
    }
  }

  rowClickHandler(e) {
    /* Handle DataTableRow clicks
       Add or remove a row index from this.state.selectedIdRowMap and
       update the selection action button disabled state.
     */
    const { state } = this;
    const { rowClickEnabled } = this.props;
    const { shiftKey, target } = e;

    // Abort if click was not inside a <tr>.
    const tr = target.closest("tr");
    if (!tr) {
      return;
    }

    const { checkbox } = tr.refs;
    const { i: rowIndex, id: rowId } = tr.dataset;
    const rowIndexInt = parseInt(rowIndex, 10);
    const { tagName } = target;
    let dispatch = false;
    if (target === checkbox) {
      // Click was on the selection checkbox.
      dispatch = true;
    } else if (
      rowClickEnabled &&
      tagName !== "INPUT" &&
      tagName !== "BUTTON" &&
      tagName !== "A" &&
      target.closest("a") === null // not child of <a>
    ) {
      // Row click is enabled and click was on eligible element.
      dispatch = true;
      // Toggle the row selection state.
      tr.selected = !tr.selected;
    }
    // Maybe dispatch a SELECTION_CHANGE event.
    const { selected } = tr;
    if (dispatch) {
      const { selectedIdRowMap } = state;
      const { rows } = state;
      if (selected) {
        selectedIdRowMap.set(rowId, rows[rowIndexInt]);
      } else {
        selectedIdRowMap.delete(rowId);
      }

      // If the shift key was pressed, set all of the rows between the last click index
      // and this one to the same selected state as the current index.
      if (shiftKey && state.lastRowClickIndex !== undefined) {
        const { rowElements } = this.state;
        const step = state.lastRowClickIndex < rowIndexInt ? 1 : -1;
        for (let i = state.lastRowClickIndex; i !== rowIndexInt; i += step) {
          const rowElement = rowElements[i];
          // Skip removed and unselectable rows.
          if (
            rowElement.classList.contains("removed") ||
            rowElement.classList.contains("unselectable")
          ) {
            // eslint-disable-next-line no-continue
            continue;
          }
          rowElement.selected = selected;
          const { id: otherRowId } = rowElement.dataset;
          if (selected) {
            selectedIdRowMap.set(otherRowId, rows[i]);
          } else {
            selectedIdRowMap.delete(otherRowId);
          }
        }
      }

      // Save the current row index as the last.
      state.lastRowClickIndex = rowIndexInt;

      this.postSelectionChangeHandler();
    }
  }

  deleteRowHandler(e) {
    /* Replace a deleted row with a message.
     */
    const { selectedIdRowMap } = this.state;
    const { rowId, message } = e.detail;
    const tr = this.tbody.querySelector(`tr[data-id="${rowId}"]`);
    tr.classList.add("removed");
    // Maybe remove the row from the selection set.
    if (selectedIdRowMap.has(rowId)) {
      selectedIdRowMap.delete(rowId);
      this.postSelectionChangeHandler();
    }
    tr.innerHTML = html`
      <td
        colspan="${this.props.columns.length + (this.props.selectable ? 1 : 0)}"
      >
        <div>${message}</div>
      </td>
    `;
    // Kick off a new total hits query.
    this.doTotalHitsQuery();
  }

  handleHeaderSortClick(button) {
    // Attempt to get the column name to sort by for the
    // given index.
    const column = this.props.columns[parseInt(button.dataset.i, 10)];
    if (!column) {
      // A value of undefined indicates that there's not a 1-to-1
      // mapping of row field to column value so let's do nothing.
      return;
    }
    // Get the column's sort param name.
    const sortParam = this.props.columnSortParamMap[column] || column;
    // If already sorted by this column, toggle to descending, otherwise
    // set to column ascending.
    this.state.search.sort =
      this.state.search.sort === sortParam ? `-${sortParam}` : sortParam;
    // Update the sort icons.
    const buttons = this.thead.querySelectorAll("button");
    buttons.forEach((el) => el.classList.remove("sort-asc", "sort-desc"));
    const isDesc = this.state.search.sort.startsWith("-");
    button.classList.add(isDesc ? "sort-desc" : "sort-asc");
    button.setAttribute(
      "aria-label",
      `sort by ${column} ${isDesc ? "ascending" : "descending"}`
    );
    button.blur();
    // Perform a search using the new sort spec.
    this.doSearch();
  }

  theadClickHandler(e) {
    /* Handle clicks within the <thead> element.
     */
    e.stopPropagation();
    const { target } = e;

    // Handle column sort.
    if (target.tagName === "BUTTON" && target.classList.contains("sort")) {
      this.handleHeaderSortClick(target);
    }
  }

  filterSelectionHandler(e) {
    /* Add or remove a column value from this.state.search.filters and
       execute a new search.
     */
    e.stopPropagation();
    const { type } = e;
    const { column, value } = e.detail;
    const { filters } = this.state.search;
    // Get the existing filter value set or init a new one.
    const valueSet = filters[column] || new Set();

    if (type === "filter-deselect-all") {
      // Remove all values from the set.
      valueSet.clear();
    } else {
      // Add or remove a single value from the set.
      valueSet[type === "filter-select" ? "add" : "delete"](value);
    }

    // Assign or delete the values set to/from filters.
    if (valueSet.size > 0) {
      filters[column] = valueSet;
    } else {
      delete filters[column];
    }

    // Execute a new search.
    this.doSearch();
  }

  updateDownloadLinkHref() {
    /* Update the downloadLink href value based on the current
       search state.
     */
    const {
      API_BASE_URL,
      csvDownloadHeaderColumnPairs,
      csvDownloadExtraQueryParams,
    } = this.props;
    const params = this.buildRequestParams();
    // Remove any offset param and set limit to -1.
    params.delete("offset");
    params.set("limit", -1);
    // Set the format.
    params.set("format", "csv");
    // Add any extra CSV download query params.
    Object.entries(csvDownloadExtraQueryParams).forEach(([k, v]) =>
      params.append(k, v)
    );
    // Build CSV header and columns fields to params.
    // Note that archiveit.renderers uses request.GET.getlist() to retrieve
    // the show_field and csv_header params, so we need to manually append
    // those here.
    const headersColumnsStr = csvDownloadHeaderColumnPairs
      .map(([header, column]) => `csv_header=${header}&show_field=${column}`)
      .join("&");
    this.downloadLink.href = `${API_BASE_URL}${this.buildAPIPath(
      params
    )}&${headersColumnsStr}`;
  }

  selectAllOnPage() {
    /* Add all rows on the current page to the active selection set. */
    const { selectedIdRowMap, rows, rowElements } = this.state;
    let anyUpdates = false;
    rows.forEach((row, i) => {
      const { _dataTableRowId: rowId } = row;
      const tr = rowElements[i];
      // Do not select "removed" or "unselectable" rows.
      if (
        !selectedIdRowMap.has(rowId) &&
        !tr.classList.contains("removed") &&
        !tr.classList.contains("unselectable")
      ) {
        anyUpdates = true;
        // Add it to the selection map.
        selectedIdRowMap.set(rowId, row);
        // Check the checkbox.
        tr.selected = true;
      }
    });
    if (anyUpdates) {
      this.postSelectionChangeHandler();
    }
  }

  async selectAllMatching() {
    /* Add all search results to the selection set. */
    const { API, selectAllExtraQueryParams } = this.props;
    const { rowElements, search, selectedIdRowMap } = this.state;
    const { selectAllCheckbox } = this.refs;
    const { numHits } = search;
    // Get the current search params.
    const params = Object.assign(this.buildRequestParams());
    // Set any select-all specific params.
    if (selectAllExtraQueryParams) {
      Object.entries(selectAllExtraQueryParams).forEach(([k, v]) =>
        params.set(k, v)
      );
    }
    // Set the limit to numHits and delete any page offset.
    params.delete("offset");
    params.set("limit", `${numHits}`);
    // Fetch all of the matching rows.
    // Show the loading popover since this may take a while.
    selectAllCheckbox.loading = true;
    const response = await API.get(this.buildAPIPath(params));
    const rows = await response.json();
    // Check for an error response.
    if (!Array.isArray(rows) && rows.error) {
      // Hide the loading popover.
      selectAllCheckbox.loading = false;
      throw new Error(rows.error);
    }
    // Add each row to the selection set.
    for (const row of rows) {
      this.enrichRowObject(row);
      selectedIdRowMap.set(row._dataTableRowId, row);
    }
    // Hide the loading popover.
    selectAllCheckbox.loading = false;
    // Ensure that every visible, selectable row checkbox is checked.
    rowElements.forEach((tr) => {
      tr.selected = !tr.classList.contains("unselectable");
    });
    // Update the selection-related UI.
    this.postSelectionChangeHandler();
  }

  selectAllCheckboxSubmitHandler(e) {
    /* Handle a select-all checkbox list item selection event. */
    e.stopPropagation();
    const { action } = e.detail;
    switch (action) {
      case "SELECT_NONE":
        this.clearSelections();
        break;
      case "SELECT_PAGE":
        this.selectAllOnPage();
        break;
      case "SELECT_ALL":
        this.selectAllMatching();
        break;
      default:
        break;
    }
  }
}
