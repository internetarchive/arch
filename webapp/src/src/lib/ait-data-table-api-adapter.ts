import { DataTable } from "./webservices/src/aitDataTable/src/types";

export default class API<RowT> {
  dataTable: DataTable<RowT>;

  constructor(dataTable: DataTable<RowT>) {
    this.dataTable = dataTable;

    // Make DataTable.doTotalHitsQuery() a noop because Vault API responses include
    // a 'count' property, which we will use to manually update the DataTable hit
    // count in DataTableAPIAdapter.get().
    dataTable.doTotalHitsQuery = () => new Promise(() => null);
  }

  async get(apiPath: string) {
    // Construct a dummy URL value from the API path.
    const url = new URL(`http://fake.com${apiPath}`);
    const { searchParams } = url;

    // Remove unsupported params.
    searchParams.delete("limit");
    searchParams.delete("offset");
    searchParams.delete("search_fields");
    searchParams.delete("sort");

    // Extract the final API path.
    apiPath = url.href.slice(url.origin.length);

    // Make the request.
    const response = await fetch(
      `${this.dataTable.props.apiBaseUrl}${apiPath}`,
      {
        credentials: "same-origin",
        headers: {
          accept: "application/json",
        },
      }
    );

    // Clone the response and read the results count.
    const count = ((await response.clone().json()) as Array<RowT>).length;

    // If request was not a facets query (as indicated by the presence of the
    // _distinct, _pluck_ _flat URL params), update the dataTable hit counts.
    const wasFacetsQuery =
      searchParams.has("_distinct") &&
      searchParams.has("_pluck") &&
      searchParams.has("_flat");
    if (!wasFacetsQuery) {
      const dataTable = this.dataTable;
      const { selectable } = dataTable.props;
      const { search } = dataTable.state;
      search.numHits = count;
      if (selectable) {
        const { selectAllCheckbox } = dataTable.refs;
        selectAllCheckbox.numHits = count;
      }
      void dataTable.updatePaginator();
    }

    // Return the response.
    return response;
  }
}
