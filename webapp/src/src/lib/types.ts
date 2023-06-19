import { JSONSchemaType } from "ajv";

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
  category: string;
  collectionId: string;
  collectionName: string;
  finishedTime?: Date;
  id: string;
  isSample: boolean;
  jobId: string;
  name: string;
  numFiles: number;
  sample: number; // -1 (not sample) or > 0 (sample)
  startTime?: Date;
  state: string;
};

export enum JobId {
  ArsLgaGeneration = "ArsLgaGeneration",
  ArsWaneGeneration = "ArsWaneGeneration",
  ArsWatGeneration = "ArsWatGeneration",
  AudioInformationExtraction = "AudioInformationExtraction",
  DomainFrequencyExtraction = "DomainFrequencyExtraction",
  DomainGraphExtraction = "DomainGraphExtraction",
  ImageGraphExtraction = "ImageGraphExtraction",
  ImageInformationExtraction = "ImageInformationExtraction",
  PdfInformationExtraction = "PdfInformationExtraction",
  PresentationProgramInformationExtraction = "PresentationProgramInformationExtraction",
  SpreadsheetInformationExtraction = "SpreadsheetInformationExtraction",
  TextFilesInformationExtraction = "TextFilesInformationExtraction",
  VideoInformationExtraction = "VideoInformationExtraction",
  WebGraphExtraction = "WebGraphExtraction",
  WebPagesExtraction = "WebPagesExtraction",
  WordProcessorInformationExtraction = "WordProcessorInformationExtraction",
}

export type Job = {
  id: JobId;
  name: string;
  description: string;
};

export type AvailableJobs = Array<{
  categoryName: string;
  categoryDescription: string;
  categoryImage: string;
  categoryId: string;
  jobs: Array<Job>;
}>;

export enum ProcessingState {
  NotStarted = "Not started",
  Queued = "Queued",
  Running = "Running",
  Finished = "Finished",
  Failed = "Failed",
}

export type JobState = {
  id: string;
  name: string;
  sample: number;
  state: ProcessingState;
  started: boolean;
  finished: boolean;
  failed: boolean;
  activeStage: string;
  activeState: ProcessingState;
  startTime?: string;
  finishedTime?: string;
};

export type PublishedDatasetInfo = {
  item: string;
  source: string;
  collection: string;
  job: JobId;
  complete: boolean;
  sample: boolean;
  time: Date;
  ark: string;
};

// Metadata keys will be displayed in the order define here.
export enum PublishedDatasetMetadataKeys {
  title = "title",
  description = "description",
  creator = "creator",
  subject = "subject",
  licenseurl = "licenseurl",
}

export type PublishedDatasetMetadataValue = string | Array<string>;

export type PublishedDatasetMetadata = Partial<
  Record<PublishedDatasetMetadataKeys, PublishedDatasetMetadataValue>
>;

export type PublishedDatasetMetadataJSONSchema = JSONSchemaType<{
  creator?: PublishedDatasetMetadataValue;
  description?: PublishedDatasetMetadataValue;
  licenseurl?: PublishedDatasetMetadataValue;
  subject?: PublishedDatasetMetadataValue;
  title?: PublishedDatasetMetadataValue;
}>;
