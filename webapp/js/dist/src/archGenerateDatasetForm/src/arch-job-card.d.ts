import { LitElement } from "lit";
import { Job, JobState } from "../../lib/types";
export declare enum JobButtonType {
    Generate = "generate",
    View = "view",
    Status = "status"
}
export declare class ArchJobCard extends LitElement {
    collectionId: string;
    job: Job;
    jobStates: Record<string, JobState>;
    createRenderRoot(): this;
    private jobStateToButtonProps;
    render(): import("lit-html").TemplateResult<1>;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-job-card": ArchJobCard;
    }
}
