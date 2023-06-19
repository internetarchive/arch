export * from "./webservices/src/lib/helpers";
import { Collection, Dataset } from "./types";
export declare const Paths: {
    collection: (id: Collection["id"]) => string;
    dataset: (id: Dataset["id"], sample: Dataset["sample"]) => string;
    generateCollectionDataset: (collectionId: Collection["id"]) => string;
    buildSubCollection: (sourceCollectionIds?: Array<Collection["id"]>) => string;
};
