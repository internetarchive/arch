import { LitElement, html } from "lit";
import { customElement, query, queryAll, state } from "lit/decorators.js";

import ArchAPI from "../../lib/ArchAPI";
import {
  AvailableJobs,
  Collection,
  FilteredApiResponse,
  JobState,
  ProcessingState,
} from "../../lib/types";
import { SAMPLE_JOB_ID_SUFFIX } from "../../lib/constants";
import { AlertClass, ArchAlert } from "../../archAlert/index";
import "./arch-job-category-section";
import { ArchJobCategorySection } from "./arch-job-category-section";
import { JobButtonType } from "./arch-job-card";

import Styles from "./styles";

@customElement("arch-generate-dataset-form")
export class ArchGenerateDatasetForm extends LitElement {
  @state() collections: null | Array<Collection> = null;
  @state() availableJobs: AvailableJobs = [];
  @state() sourceCollectionId: string | null = null;
  @state() jobStates: Record<string, Record<string, JobState>> = {};
  @state() activePollCollectionId: string | null = null;
  @state() anyErrors = false;

  @query("select[name=source-collection]")
  collectionSelector!: HTMLSelectElement;
  @query("arch-alert.error") errorAlert!: ArchAlert;
  @query("arch-alert.email") emailAlert!: ArchAlert;
  @queryAll("arch-job-category-section")
  categorySections!: Array<ArchJobCategorySection>;

  // Apply any ARCH-specific styles.
  static styles = Styles;

  static urlCollectionParamName = "cid";

  async connectedCallback() {
    // Fetch available Collections and Jobs.
    await this.initAvailableJobs();
    void this.initCollections();
    super.connectedCallback();
    this.addEventListener("click", (e: Event) => void this.clickHandler(e));
  }

  createRenderRoot() {
    /* Disable the shadow root for this component to let in global styles.
       https://stackoverflow.com/a/55213037 */
    return this;
  }

  render() {
    const collectionJobStates =
      this.sourceCollectionId && this.jobStates[this.sourceCollectionId];
    return html`
      <label for="source-collection">Select Source Collection</label>
      <select
        name="source-collection"
        @change=${this.sourceCollectionChangeHandler}
        ?disabled=${this.collections === null}
      >
        ${this.collections === null
          ? html`<option>Loading...</option>`
          : html`<option value="">~ Choose Source Collection ~</option>`}
        ${(this.collections ?? []).map(
          (collection) => html`
            <option
              value="${collection.id}"
              ?selected=${collection.id === this.sourceCollectionId}
            >
              ${collection.name}
            </option>
          `
        )}
      </select>

      <arch-alert
        class="sample"
        alertClass=${AlertClass.Secondary}
        message="Sample datasets can be quickly generated in order to ensure that the analysis will produce datasets that meet your needs. These datasets use the first 100 relative records from the collection if they are available. We strongly recommend generating samples for any collections over 100GB."
      ></arch-alert>

      <arch-alert
        class="email"
        alertClass=${AlertClass.Primary}
        message="ARCH is creating your dataset. You will receive an email notification when the dataset is complete."
        hidden
      ></arch-alert>

      <arch-alert
        class="error"
        alertClass=${AlertClass.Danger}
        message="A dataset generation job has failed, and we are currently investigating it."
        ?hidden=${!this.anyErrors}
      ></arch-alert>

      ${this.availableJobs.map(
        (jobsCat, i) => html`
          <arch-job-category-section
            .collectionId=${this.sourceCollectionId}
            .jobsCat=${jobsCat}
            .jobStates=${collectionJobStates}
            ?collapsed=${i > 0}
          >
          </arch-job-category-section>
        `
      )}
    `;
  }

  private setCollectionIdUrlParam(collectionId: Collection["id"]) {
    const { urlCollectionParamName } = ArchGenerateDatasetForm;
    const url = new URL(window.location.href);
    if (!collectionId) {
      url.searchParams.delete(urlCollectionParamName);
    } else {
      url.searchParams.set(urlCollectionParamName, collectionId);
    }
    history.replaceState(null, "", url.toString());
  }

  private async sourceCollectionChangeHandler(e: Event) {
    const collectionId = (e.target as HTMLSelectElement).value;
    this.setCollectionIdUrlParam(collectionId);
    await this.setSourceCollectionId(collectionId);
    this.requestUpdate();
  }

  private updateAnyErrors() {
    /* If collectionId is set, set anyErrors=true if any job failed, otherwise set
     * anyErrors=false
     */
    const collectionId = this.sourceCollectionId as string;
    if (collectionId) {
      for (const jobState of Object.values(this.jobStates[collectionId])) {
        if (jobState.state === ProcessingState.Failed) {
          this.anyErrors = true;
          return;
        }
      }
    }
    this.anyErrors = false;
  }

  private async setSourceCollectionId(collectionId: string) {
    this.sourceCollectionId = collectionId;
    // If a collection is selected, fetch the job states.
    if (collectionId) {
      this.jobStates[collectionId] = await this.fetchCollectionJobStates(
        collectionId
      );
    }
    // Update the visibility of the error alert.
    this.updateAnyErrors();
  }

  private async initCollections() {
    const response =
      (await ArchAPI.collections.get()) as FilteredApiResponse<Collection>;
    this.collections = response.results;
    // Maybe select an initial Collection.
    const initialCollectionId = new URLSearchParams(window.location.search).get(
      ArchGenerateDatasetForm.urlCollectionParamName
    );
    if (initialCollectionId) {
      await this.setSourceCollectionId(initialCollectionId);
      this.requestUpdate();
    }
  }

  private async initAvailableJobs() {
    this.availableJobs = (await (
      await fetch("/api/available-jobs")
    ).json()) as AvailableJobs;
  }

  private async fetchCollectionJobStates(collectionId: string) {
    const data = (await (
      await fetch(`/api/jobstates/${collectionId}?all=true`)
    ).json()) as Array<JobState>;
    // Transform the array of JobStates into an object keyed by job.id with sample-type
    // jobs having an appended SAMPLE_JOB_ID_SUFFIX.
    return Object.fromEntries(
      data.map((x: JobState) => [
        `${x.id}${x.sample > 0 ? SAMPLE_JOB_ID_SUFFIX : ""}`,
        x,
      ])
    );
  }

  async pollJobStates() {
    const collectionId = this.sourceCollectionId as string;
    // Stop polling if the selected collection has changed.
    if (this.activePollCollectionId !== collectionId) {
      this.activePollCollectionId = null;
      return;
    }
    // Fetch the current collection job states.
    this.jobStates[collectionId] = await this.fetchCollectionJobStates(
      collectionId
    );
    // Show the error alert if any jobs failed.
    this.updateAnyErrors();
    // Request a lit component update.
    this.requestUpdate();
    // Keep polling if any jobs remain active.
    for (const jobState of Object.values(this.jobStates[collectionId])) {
      if (jobState.state === ProcessingState.Running) {
        // A job is active, set a polling timeout and return.
        setTimeout(() => void this.pollJobStates(), 2000);
        return;
      }
    }
    // No jobs remain active, so stop polling.
    this.activePollCollectionId = null;
  }

  private startPolling() {
    // Abort if polling is already active.
    if (this.activePollCollectionId !== null) {
      return;
    }
    this.activePollCollectionId = this.sourceCollectionId;
    void this.pollJobStates();
  }

  private expandCategorySection(categorySection: ArchJobCategorySection) {
    this.categorySections.forEach((el) => {
      if (el === categorySection) {
        el.expand();
      } else {
        el.collapse();
      }
    });
  }

  private async runJob(jobId: string, sample: boolean) {
    await fetch(
      `/api/runjob/${jobId}/${this.sourceCollectionId as string}${
        sample ? "?sample=true" : ""
      }`
    );
  }

  private async clickHandler(e: Event) {
    const target = e.target as HTMLElement;
    // Handle a collapsed category section click.
    const categorySection = target.closest("arch-job-category-section");
    if (categorySection?.collapsed) {
      this.expandCategorySection(categorySection);
      return;
    }

    // Handle a job button click.
    if (target.tagName === "BUTTON") {
      const {
        jobId,
        buttonType,
        sample: sampleStr,
      } = target.dataset as {
        jobId: string;
        buttonType: string;
        sample: string;
      };
      // Cast presence / absence of sample value to bool.
      const sample = sampleStr !== undefined;
      switch (buttonType) {
        case JobButtonType.Generate:
          // Run a job.
          await this.runJob(jobId, sample);
          this.emailAlert.show();
          this.startPolling();
          break;
        case JobButtonType.View:
          // View a dataset.
          window.location.href = `/datasets/${
            this.sourceCollectionId as string
          }:${jobId}${sample ? "?sample=true" : ""}`;
          break;
      }
      return;
    }
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-generate-dataset-form": ArchGenerateDatasetForm;
  }
}
