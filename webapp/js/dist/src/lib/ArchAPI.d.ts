import { ApiParams, ApiPath, ApiResponse, Collection, Dataset } from "./types";
export default class ArchAPI {
    static BasePath: string;
    static get<T>(path: ApiPath, params?: ApiParams<T>): Promise<ApiResponse<T>>;
    static get collections(): {
        get: (params?: ApiParams<Collection>) => Promise<ApiResponse<Collection>>;
    };
    static get datasets(): {
        get: (params?: ApiParams<Dataset>) => Promise<ApiResponse<Dataset>>;
    };
}
