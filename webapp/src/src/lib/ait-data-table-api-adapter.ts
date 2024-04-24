import { FilteredApiResponse, DistinctApiResponse } from "./types";

import { DataTable } from "./webservices/src/aitDataTable/src/types";

export default class API<RowT> {
  dataTable: DataTable<RowT>;

  constructor(dataTable: DataTable<RowT>) {
    this.dataTable = dataTable;

    // Make DataTable.doTotalHitsQuery() a noop because ARCH API responses include
    // a 'count' property, which we will use to manually update the DataTable hit
    // count in DataTableAPIAdapter.get().
    dataTable.doTotalHitsQuery = () => new Promise(() => null);
  }

  async get(apiPath: string) {
    // Construct a dummy URL value from the API path.
    const url = new URL(`http://fake.com${apiPath}`);
    const { searchParams } = url;

    // Remove unsupported params.
    searchParams.delete("search_fields");

    // Remove limit=-1
    if (searchParams.get("limit") === "-1") {
      searchParams.delete("limit");
    }

    // Replace te Django-style {field}__in={csv} params with discrete
    // {field}={value} params.
    Array.from(searchParams.keys())
      .filter((k) => k.endsWith("__in"))
      .forEach((k) => {
        const finalK = k.slice(0, k.length - 4);
        (searchParams.get(k) as string)
          .split(",")
          .forEach((v) => searchParams.append(finalK, v));
        searchParams.delete(k);
      });

    // Extract the final API path.
    apiPath = url.href.slice(url.origin.length);

    // Make the request.
    const response = (await (
      await fetch(`${this.dataTable.props.apiBaseUrl}${apiPath}`, {
        credentials: "same-origin",
        headers: {
          accept: "application/json",
        },
      })
    ).json()) as FilteredApiResponse<RowT> | DistinctApiResponse<RowT>;

    // Read the total results count.
    const { count } = response;

    // If request was not a distinct / facets query, update the dataTable hit counts.
    if (!searchParams.has("distinct")) {
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

    // The DataTable expects a Response-type object, for which json() will
    // return the requested object, i.e. the 'results' part of the ARCH API
    // response. So let's give it what it wants.
    return {
      json: () => Promise.resolve(response.results),
    };
  }
}
