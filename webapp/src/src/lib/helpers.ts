export * from "./webservices/src/lib/helpers";
import { Collection, Dataset } from "./types";
import { ArchSubCollectionBuilder } from "../archSubCollectionBuilder/index";
import { ArchGenerateDatasetForm } from "../archGenerateDatasetForm/index";

const _ = encodeURIComponent;

export const Paths = {
  collection: (id: Collection["id"]) => `/collections/${_(id)}`,

  dataset: (id: Dataset["id"], sample: Dataset["sample"]) =>
    `/datasets/${_(id)}?sample=${sample > -1 ? "true" : "false"}`,

  generateCollectionDataset: (collectionId: Collection["id"]) =>
    `/datasets/generate?${_(
      ArchGenerateDatasetForm.urlCollectionParamName
    )}=${_(collectionId)}`,

  buildSubCollection: (sourceCollectionIds?: Array<Collection["id"]>) =>
    sourceCollectionIds === undefined
      ? "/collections/custom-collection-builder"
      : `/collections/custom-collection-builder?${sourceCollectionIds
          .map(
            (x) =>
              `${_(ArchSubCollectionBuilder.urlCollectionsParamName)}=${_(x)}`
          )
          .join("&")}`,
};
