import { LitElement } from "lit";
import { customElement, property, query } from "lit/decorators.js";

import DataTable from "../../legacy/data-table/data-table.js";

import { Topics } from "../../lib/pubsub.js";

import Styles from "./styles.js";
import {
  ApiConstructorType,
  DataTable as DataTableType,
  DataTableEvent,
  NativeDataTableEvents,
  SelectionActionEventData,
  SelectionChangeEventData,
} from "./types.js";

type PropsT<RowT> = DataTableType<RowT>["props"];

class DefaultAPI<RowT> {
  dataTable: DataTableType<RowT>;

  constructor(dataTable: DataTableType<RowT>) {
    this.dataTable = dataTable;
  }

  get(apiPath: string) {
    return fetch(`${this.dataTable.props.apiBaseUrl}${apiPath}`, {
      credentials: "same-origin",
      headers: {
        accept: "application/json",
      },
    });
  }
}

@customElement("ait-data-table")
export class AitDataTable<RowT> extends LitElement {
  @property({ type: Array })
  actionButtonClasses: PropsT<RowT>["actionButtonClasses"] = [];
  @property({ type: Array })
  actionButtonLabels: PropsT<RowT>["actionButtonLabels"] = [];
  @property({ type: Array })
  actionButtonDisabled: PropsT<RowT>["actionButtonDisabled"] = [];
  @property({ type: Array })
  actionButtonSignals: PropsT<RowT>["actionButtonSignals"] = [];
  @property() apiBaseUrl: PropsT<RowT>["apiBaseUrl"] = "/api";
  apiFactory: ApiConstructorType<RowT> = DefaultAPI;
  @property() apiCollectionEndpoint!: PropsT<RowT>["apiCollectionEndpoint"];
  @property() apiItemResponseIsArray: PropsT<RowT>["apiItemResponseIsArray"] =
    false;
  @property() apiItemTemplate: PropsT<RowT>["apiItemTemplate"] = null;
  @property({ type: Array })
  apiStaticParamPairs: PropsT<RowT>["apiStaticParamPairs"] = [];
  @property() cellRenderers: PropsT<RowT>["cellRenderers"] = [];
  @property({ type: Array }) columnHeaders: PropsT<RowT>["columnHeaders"] = [];
  @property({ type: Array }) columns!: PropsT<RowT>["columns"];
  @property({ type: Object })
  columnSortParamMap: PropsT<RowT>["columnSortParamMap"] = {};
  @property({ type: Array })
  filterableColumns: PropsT<RowT>["filterableColumns"] = [];
  @property({ type: Array })
  columnFilterDisplayMaps: PropsT<RowT>["columnFilterDisplayMaps"] = [];
  @property({ type: Array })
  columnFilterParams: PropsT<RowT>["columnFilterParams"] = [];
  @property() itemPollPredicate: PropsT<RowT>["itemPollPredicate"] = null;
  @property({ type: Number })
  itemPollPeriodSeconds: PropsT<RowT>["itemPollPeriodSeconds"] = 3;
  @property({ type: String })
  loadingMessage: PropsT<RowT>["loadingMessage"] = "Loading...";
  @property({ type: Boolean })
  noInitialSearch: PropsT<RowT>["noInitialSearch"] = false;
  @property({ type: Array })
  nonSelectionActionLabels: PropsT<RowT>["nonSelectionActionLabels"] = [];
  @property({ type: Array })
  nonSelectionActions: PropsT<RowT>["nonSelectionActions"] = [];
  @property() noResultsText: PropsT<RowT>["noResultsText"] = "No Results";
  @property() nullString: PropsT<RowT>["nullString"] = "&mdash;";
  @property({ type: Number }) pageLength: PropsT<RowT>["pageLength"] = 100;
  @property() pluralName: PropsT<RowT>["pluralName"] = "Items";
  @property({ type: Boolean })
  persistSearchStateInUrl: PropsT<RowT>["persistSearchStateInUrl"] = false;
  @property() props: PropsT<RowT> | Record<string, unknown> = {};
  @property({ type: Boolean })
  rowClickEnabled: PropsT<RowT>["rowClickEnabled"] = false;
  @property() rowIdColumn: PropsT<RowT>["rowIdColumn"] = "id";
  @property({ type: Array }) searchColumns: PropsT<RowT>["searchColumns"] = [];
  @property({ type: Array })
  searchColumnLabels: PropsT<RowT>["searchColumnLabels"] = [];
  @property({ type: Object })
  selectAllExtraQueryParams: PropsT<RowT>["selectAllExtraQueryParams"] = null;
  @property({ type: Boolean }) selectable: PropsT<RowT>["selectable"] = false;
  @property() singleName: PropsT<RowT>["singleName"] = "Item";
  @property() sort: PropsT<RowT>["sort"] = "-id";
  @property({ type: Array }) sortableColumns: PropsT<RowT>["sortableColumns"] =
    [];

  @query("data-table") dataTable!: DataTableType<RowT>;

  static styles = Styles;

  render(): HTMLElement | Array<HTMLElement> {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-call
    const dataTable = new DataTable() as unknown as DataTableType<RowT>;
    dataTable.props = Object.assign(
      {
        API: new this.apiFactory(dataTable),
        actionButtonClasses: this.actionButtonClasses,
        actionButtonLabels: this.actionButtonLabels,
        actionButtonDisabled: this.actionButtonDisabled,
        actionButtonSignals: this.actionButtonSignals,
        apiBaseUrl: this.apiBaseUrl,
        apiCollectionEndpoint: this.apiCollectionEndpoint,
        apiItemResponseIsArray: this.apiItemResponseIsArray,
        apiItemTemplate: this.apiItemTemplate,
        apiStaticParamPairs: this.apiStaticParamPairs,
        cellRenderers: this.cellRenderers,
        columnHeaders: this.columnHeaders,
        columns: this.columns,
        columnSortParamMap: this.columnSortParamMap,
        filterableColumns: this.filterableColumns,
        columnFilterDisplayMaps: this.columnFilterDisplayMaps,
        columnFilterParams: this.columnFilterParams,
        loadingMessage: this.loadingMessage,
        itemPollPeriodSeconds: this.itemPollPeriodSeconds,
        itemPollPredicate: this.itemPollPredicate,
        noInitialSearch: this.noInitialSearch,
        nonSelectionActionLabels: this.nonSelectionActionLabels,
        nonSelectionActions: this.nonSelectionActions,
        noResultsText: this.noResultsText,
        nullString: this.nullString,
        pageLength: this.pageLength,
        persistSearchStateInUrl: this.persistSearchStateInUrl,
        pluralName: this.pluralName,
        rowClickEnabled: this.rowClickEnabled,
        rowIdColumn: this.rowIdColumn,
        searchColumns: this.searchColumns,
        searchColumnLabels: this.searchColumnLabels,
        selectAllExtraQueryParams: this.selectAllExtraQueryParams,
        selectable: this.selectable,
        singleName: this.singleName,
        sort: this.sort,
        sortableColumns: this.sortableColumns,
      },
      this.props || {}
    );

    dataTable.addEventListener("submit", (e) =>
      this.dataTableEventReceiver(e as unknown as DataTableEvent<RowT>)
    );
    return dataTable;
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  nonSelectionActionHandler(action: string) {
    /* Override in subclass. */
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  selectionActionHandler(action: string, selectedRows: Array<RowT>) {
    /* Override in subclass. */
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  postSelectionChangeHandler(selectedRows: Array<RowT>) {
    /* Override in subclass. */
  }

  dataTableEventReceiver(e: DataTableEvent<RowT>) {
    const { signal, data } = e.detail;
    const { nonSelectionActions, actionButtonSignals } = this;
    if (nonSelectionActions.includes(signal as Topics)) {
      this.nonSelectionActionHandler(signal);
    } else if (actionButtonSignals.includes(signal as Topics) && data) {
      this.selectionActionHandler(
        signal,
        data as SelectionActionEventData<RowT>
      );
    } else if (signal === NativeDataTableEvents.SELECTION_CHANGE && data) {
      const { selectedRows } = data as SelectionChangeEventData<RowT>;
      this.postSelectionChangeHandler(selectedRows);
    } else {
      // ignore
    }
  }
}
