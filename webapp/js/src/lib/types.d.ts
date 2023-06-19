export type Collection = {
    id: string;
    name: string;
    public: boolean;
    lastJobId?: string;
    lastJobSample?: boolean;
    lastJobTime?: Date;
    size: string;
    sortSize: number;
    seeds: number;
    lastCrawlDate: Date;
};
export type Dataset = {
    collectionId: string;
    collectionName: string;
    id: string;
    category: string;
    name: string;
    state: string;
    startTime?: Date;
    finishedTime?: Date;
    numFiles: number;
};
