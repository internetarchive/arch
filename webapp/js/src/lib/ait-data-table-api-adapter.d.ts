import { DataTable } from "./webservices/src/aitDataTable/src/types";
export default class API<RowT> {
    dataTable: DataTable<RowT>;
    constructor(dataTable: DataTable<RowT>);
    get(apiPath: string): Promise<Response>;
}
