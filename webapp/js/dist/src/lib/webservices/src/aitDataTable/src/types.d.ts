import { ValueOf } from "../../lib/helpers.js";
import { Topics } from "../../lib/pubsub.js";
type EnrichedDataTableRow<RowT> = RowT & {
    _dataTableRowId: number;
};
export interface DataTableSelectAllCheckbox extends HTMLElement {
    numHits: number;
}
export interface ApiConstructorType<RowT> {
    new (dataTable: DataTable<RowT>): ApiType<RowT>;
}
type NormalApiResponse<RowT> = Array<RowT>;
type DistinctApiResponse<RowT> = Array<RowT[keyof RowT]>;
export interface ApiType<RowT> {
    dataTable: DataTable<RowT>;
    get: (apiPath: string) => Promise<{
        json: () => Promise<NormalApiResponse<RowT> | DistinctApiResponse<RowT>>;
    }>;
}
export interface DataTable<RowT> extends HTMLElement {
    state: {
        rows: Array<EnrichedDataTableRow<RowT>>;
        search: {
            numHits: number;
        };
        selectedIdRowMap: Map<string, RowT>;
    };
    props: {
        API: ApiType<RowT>;
        apiBaseUrl: string;
        actionButtonClasses: Array<string>;
        actionButtonLabels: Array<string>;
        actionButtonDisabled: Array<boolean>;
        actionButtonSignals: Array<Topics>;
        apiCollectionEndpoint: string;
        apiItemResponseIsArray: boolean;
        apiItemTemplate: string | null;
        apiStaticParamPairs: Array<[string, string]>;
        cellRenderers: Array<((field: ValueOf<RowT>, row: RowT) => string) | Record<string, string> | undefined>;
        columnHeaders: Array<string>;
        columns: Array<string>;
        columnSortParamMap: Record<string, string>;
        filterableColumns: Array<boolean>;
        columnFilterDisplayMaps: Array<Record<string, string> | undefined>;
        columnFilterParams: Array<Record<string, string> | undefined>;
        itemPollPredicate: ((row: RowT) => boolean) | null;
        itemPollPeriodSeconds: number;
        loadingMessage: string;
        noInitialSearch: boolean;
        nonSelectionActionLabels: Array<string>;
        nonSelectionActions: Array<Topics>;
        noResultsText: string;
        nullString: string;
        pageLength: number;
        persistSearchStateInUrl: boolean;
        pluralName: string;
        rowClickEnabled: boolean;
        rowIdColumn: string;
        searchColumns: Array<string>;
        searchColumnLabels: Array<string>;
        selectAllExtraQueryParams: Record<string, string> | null;
        selectable: boolean;
        singleName: string;
        sort: string;
        sortableColumns: Array<boolean>;
    };
    refs: {
        selectAllCheckbox: DataTableSelectAllCheckbox;
    };
    doTotalHitsQuery: () => Promise<null>;
    postSelectionChangeHandler: () => void;
    setSelectionActionButtonDisabledState: (disabled: boolean) => void;
    throttledDoSearch: (resetPageNum?: boolean) => Promise<null>;
    unenrichRowObject: (row: EnrichedDataTableRow<RowT>) => RowT;
    updatePaginator: () => Promise<null>;
}
export declare enum NativeDataTableEvents {
    SELECTION_CHANGE = "SELECTION_CHANGE"
}
export type SelectionActionEventData<RowT> = Array<RowT>;
export type SelectionChangeEventData<RowT> = {
    selectedRows: Array<RowT>;
};
export type DataTableEvent<RowT> = CustomEvent<{
    signal: NativeDataTableEvents | Topics;
    data: SelectionActionEventData<RowT> | SelectionChangeEventData<RowT> | undefined;
}>;
export {};
