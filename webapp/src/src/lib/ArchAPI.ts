import { ApiParams, ApiPath, ApiResponse, Collection, Dataset } from "./types";

export default class ArchAPI {
  static BasePath = "/api";

  static async get<T>(
    path: ApiPath,
    params: ApiParams<T> = []
  ): Promise<ApiResponse<T>> {
    const searchParams = new URLSearchParams(
      params.map(([k, op, v]) => [
        `${String(k)}${op === "!=" ? "!" : ""}`,
        String(v),
      ])
    );
    return (
      await fetch(
        `${ArchAPI.BasePath}${path}${
          params ? `?${searchParams.toString()}` : ""
        }`
      )
    ).json() as Promise<ApiResponse<T>>;
  }

  static get collections() {
    return {
      get: (params: ApiParams<Collection> = []) =>
        ArchAPI.get<Collection>("/collections", params),
    };
  }

  static get datasets() {
    return {
      get: (params: ApiParams<Dataset> = []) =>
        ArchAPI.get<Dataset>("/datasets", params),
    };
  }
}
