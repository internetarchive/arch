import { LitElement } from "lit";
import { AvailableJobs, JobState } from "../../lib/types";
import "./arch-job-card";
export declare class ArchJobCategorySection extends LitElement {
    collapsed: boolean;
    collectionId: string;
    jobsCat: AvailableJobs[0];
    jobStates: Record<string, JobState>;
    createRenderRoot(): this;
    expand(): void;
    collapse(): void;
    render(): import("lit-html").TemplateResult<1>;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-job-category-section": ArchJobCategorySection;
    }
}
