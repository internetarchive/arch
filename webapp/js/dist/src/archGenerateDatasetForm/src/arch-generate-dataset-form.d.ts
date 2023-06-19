import { LitElement } from "lit";
import { AvailableJobs, Collection, JobState } from "../../lib/types";
import { ArchAlert } from "../../archAlert/index";
import "./arch-job-category-section";
import { ArchJobCategorySection } from "./arch-job-category-section";
export declare class ArchGenerateDatasetForm extends LitElement {
    collections: null | Array<Collection>;
    availableJobs: AvailableJobs;
    sourceCollectionId: string | null;
    jobStates: Record<string, Record<string, JobState>>;
    activePollCollectionId: string | null;
    anyErrors: boolean;
    collectionSelector: HTMLSelectElement;
    errorAlert: ArchAlert;
    emailAlert: ArchAlert;
    categorySections: Array<ArchJobCategorySection>;
    static styles: import("lit").CSSResult;
    static urlCollectionParamName: string;
    connectedCallback(): Promise<void>;
    createRenderRoot(): this;
    render(): import("lit-html").TemplateResult<1>;
    private setCollectionIdUrlParam;
    private sourceCollectionChangeHandler;
    private updateAnyErrors;
    private setSourceCollectionId;
    private initCollections;
    private initAvailableJobs;
    private fetchCollectionJobStates;
    pollJobStates(): Promise<void>;
    private startPolling;
    private expandCategorySection;
    private runJob;
    private clickHandler;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-generate-dataset-form": ArchGenerateDatasetForm;
    }
}
